/*
 * Copyright (c) 2014-2015. Vlad Ilyushchenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nfsdb.ql.parser;

import com.nfsdb.collections.IntStack;
import com.nfsdb.ql.model.ExprNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayDeque;
import java.util.Deque;

public class ExprParser {

    private final TokenStream toks;

    public ExprParser(TokenStream toks) {
        this.toks = toks;
        toks.defineSymbol(" ");
        toks.defineSymbol("(");
        toks.defineSymbol(")");
        toks.defineSymbol(",");
        for (int i = 0, k = Operator.operators.size(); i < k; i++) {
            Operator op = Operator.operators.getQuick(i);
            if (op.symbol) {
                toks.defineSymbol(op.token);
            }
        }
    }

    @SuppressFBWarnings({"LII_LIST_INDEXED_ITERATING"})
    public ExprParser() {
        this(new TokenStream());
    }

    public void parseExpr(CharSequence in, ExprListener listener) throws ParserException {
        toks.setContent(in);
        parseExpr(listener);
    }

    @SuppressWarnings("ConstantConditions")
    @SuppressFBWarnings({"CC_CYCLOMATIC_COMPLEXITY", "SF_SWITCH_NO_DEFAULT"})
    public void parseExpr(ExprListener listener) throws ParserException {
        Deque<ExprNode> opStack = new ArrayDeque<>();
        IntStack paramCountStack = new IntStack();

        int paramCount = 0;
        int braceCount = 0;

        ExprNode node;
        CharSequence tok;
        char thisChar = 0, prevChar;
        Branch prevBranch;
        Branch thisBranch = Branch.NONE;

        OUT:
        while ((tok = toks.optionTok()) != null) {
            prevChar = thisChar;
            thisChar = tok.charAt(0);
            prevBranch = thisBranch;

            switch (thisChar) {
                case ',':
                    thisBranch = Branch.COMMA;
                    if (prevChar == ',') {
                        throw new ParserException(toks.position(), "Missing argument");
                    }

                    if (braceCount == 0) {
                        // comma outside of braces
                        toks.unparse();
                        break OUT;
                    }

                    // If the token is a function argument separator (e.g., a comma):
                    // Until the token at the top of the stack is a left parenthesis,
                    // pop operators off the stack onto the output queue. If no left
                    // parentheses are encountered, either the separator was misplaced or
                    // parentheses were mismatched.
                    while ((node = opStack.pollFirst()) != null && node.token.charAt(0) != '(') {
                        listener.onNode(node);
                    }

                    if (node != null) {
                        opStack.push(node);
                    }

                    paramCount++;
                    break;

                case '(':
                    thisBranch = Branch.LEFT_BRACE;
                    braceCount++;
                    // If the token is a left parenthesis, then push it onto the stack.
                    paramCountStack.push(paramCount);
                    paramCount = 0;
                    opStack.push(new ExprNode(ExprNode.NodeType.CONTROL, "(", Integer.MAX_VALUE, toks.position()));
                    break;

                case ')':
                    if (prevChar == ',') {
                        throw new ParserException(toks.position(), "Missing argument");
                    }
                    if (braceCount == 0) {
                        throw new ParserException(toks.position(), "Unbalanced )");
                    }

                    thisBranch = Branch.RIGHT_BRACE;
                    braceCount--;
                    // If the token is a right parenthesis:
                    // Until the token at the top of the stack is a left parenthesis, pop operators off the stack onto the output queue.
                    // Pop the left parenthesis from the stack, but not onto the output queue.
                    //        If the token at the top of the stack is a function token, pop it onto the output queue.
                    //        If the stack runs out without finding a left parenthesis, then there are mismatched parentheses.
                    while ((node = opStack.pollFirst()) != null && node.token.charAt(0) != '(') {
                        listener.onNode(node);
                    }

                    // enable operation or literal absorb parameters
                    if ((node = opStack.peek()) != null && (node.type == ExprNode.NodeType.LITERAL || (node.type == ExprNode.NodeType.SET_OPERATION))) {
                        node.paramCount = (prevChar == '(' ? 0 : paramCount + 1) + (node.paramCount == 2 ? 1 : 0);
                        node.type = ExprNode.NodeType.FUNCTION;
                        listener.onNode(node);
                        opStack.pollFirst();
                        if (paramCountStack.notEmpty()) {
                            paramCount = paramCountStack.pop();
                        }
                    }
                    break;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case '"':
                case '\'':
                    thisBranch = Branch.CONSTANT;
                    // If the token is a number, then add it to the output queue.
                    listener.onNode(new ExprNode(ExprNode.NodeType.CONSTANT, tok.toString(), 0, toks.position()));
                    break;
                default:
                    Operator op;
                    if ((op = Operator.opMap.get(tok)) != null) {

                        thisBranch = Branch.OPERATOR;

                        // If the token is an operator, o1, then:
                        // while there is an operator token, o2, at the top of the operator stack, and either
                        // o1 is left-associative and its precedence is less than or equal to that of o2, or
                        // o1 is right associative, and has precedence less than that of o2,
                        //        then pop o2 off the operator stack, onto the output queue;
                        // push o1 onto the operator stack.

                        Operator.OperatorType type = op.type;


                        switch (thisChar) {
                            case '-':
                                switch (prevBranch) {
                                    case OPERATOR:
                                    case COMMA:
                                    case NONE:
                                        // we have unary minus
                                        type = Operator.OperatorType.UNARY;
                                }
                        }

                        ExprNode other;
                        // UNARY operators must never pop BINARY ones regardless of precedence
                        // this is to maintain correctness of -a^b
                        while ((other = opStack.peek()) != null) {
                            boolean greaterPrecedence = (op.leftAssociative && op.precedence >= other.precedence) || (!op.leftAssociative && op.precedence > other.precedence);
                            if (greaterPrecedence &&
                                    (type != Operator.OperatorType.UNARY || (type == Operator.OperatorType.UNARY && other.paramCount == 1))) {
                                listener.onNode(other);
                                opStack.pollFirst();
                            } else {
                                break;
                            }
                        }
                        node = new ExprNode(
                                op.type == Operator.OperatorType.SET ? ExprNode.NodeType.SET_OPERATION : ExprNode.NodeType.OPERATION,
                                op.token,
                                op.precedence,
                                toks.position()
                        );
                        switch (type) {
                            case UNARY:
                                node.paramCount = 1;
                                break;
                            default:
                                node.paramCount = 2;
                        }
                        opStack.push(node);
                    } else if (thisBranch != Branch.RIGHT_BRACE && thisBranch != Branch.CONSTANT && thisBranch != Branch.LITERAL) {
                        thisBranch = Branch.LITERAL;
                        // If the token is a function token, then push it onto the stack.
                        opStack.push(new ExprNode(ExprNode.NodeType.LITERAL, tok.toString(), Integer.MIN_VALUE, toks.position()));
                    } else {
                        // literal can be at start of input, after a bracket or part of an operator
                        // all other cases are illegal and will be considered end-of-input
                        toks.unparse();
                        break OUT;
                    }
            }
        }

        while ((node = opStack.poll()) != null) {
            if (node.token.charAt(0) == '(') {
                throw new ParserException(node.position, "Unbalanced (");
            }
            listener.onNode(node);
        }
    }


    private enum Branch {
        NONE, COMMA, LEFT_BRACE, RIGHT_BRACE, CONSTANT, OPERATOR, LITERAL
    }
}
