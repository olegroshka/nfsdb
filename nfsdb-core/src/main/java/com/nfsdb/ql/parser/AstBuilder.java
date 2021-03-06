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

import com.nfsdb.collections.ObjList;
import com.nfsdb.ql.model.ExprNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayDeque;
import java.util.Deque;

public class AstBuilder implements ExprListener {

    private final Deque<ExprNode> stack = new ArrayDeque<>();

    @SuppressFBWarnings({"PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS"})
    @Override
    public void onNode(ExprNode node) {
        switch (node.paramCount) {
            case 0:
                break;
            case 1:
                node.rhs = stack.pollFirst();
                break;
            case 2:
                node.rhs = stack.pollFirst();
                node.lhs = stack.pollFirst();
                break;
            default:
                ObjList<ExprNode> a = new ObjList<>(node.paramCount);
                for (int i = 0; i < node.paramCount; i++) {
                    a.add(stack.pollFirst());
                }
                node.args = a;
        }
        stack.push(node);
    }

    public void reset() {
        stack.clear();
    }

    public ExprNode root() {
        return stack.pollFirst();
    }
}
