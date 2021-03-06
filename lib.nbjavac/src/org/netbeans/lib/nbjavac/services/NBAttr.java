/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.netbeans.lib.nbjavac.services;

import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.util.Context;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lahvac
 */
public class NBAttr extends Attr {

    public static void preRegister(Context context) {
        context.put(attrKey, new Context.Factory<Attr>() {
            public Attr make(Context c) {
                return new NBAttr(c);
            }
        });
    }

    private final CancelService cancelService;

    public NBAttr(Context context) {
        super(context);
        cancelService = CancelService.instance(context);
    }

    @Override
    public void visitClassDef(JCClassDecl tree) {
        cancelService.abortIfCanceled();
        super.visitClassDef(tree);
    }

    @Override
    public void visitMethodDef(JCMethodDecl tree) {
        cancelService.abortIfCanceled();
        super.visitMethodDef(tree);
    }

    @Override
    public void visitBlock(JCBlock tree) {
        cancelService.abortIfCanceled();
        super.visitBlock(tree);
    }

    @Override
    public void visitNewClass(JCNewClass tree) {
        super.visitNewClass(tree);
        if (tree.def != null && tree.def.sym == null) {
            try {
                Field envField = Attr.class.getDeclaredField("env");
                envField.setAccessible(true);
                Env<AttrContext> env = (Env<AttrContext>) envField.get(this);
                env = env.dup(tree);
                attribStat(tree.def, env);
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(NBAttr.class.getName()).log(Level.FINE, null, ex);
            }
        }
    }

    private boolean fullyAttribute;
    private Env<AttrContext> fullyAttributeResult;

    protected void breakTreeFound(Env<AttrContext> env) {
        if (fullyAttribute) {
            fullyAttributeResult = env;
        } else {
            try {
                MethodHandles.lookup()
                             .findSpecial(Attr.class, "breakTreeFound", MethodType.methodType(void.class, Env.class), NBAttr.class)
                             .invokeExact(this, env);
            } catch (Throwable ex) {
                sneakyThrows(ex);
            }
        }
    }

    private <T extends Throwable> void sneakyThrows(Throwable t) throws T {
        throw (T) t;
    }

    public Env<AttrContext> attributeAndCapture(JCTree tree, Env<AttrContext> env, JCTree to) {
        try {
            fullyAttribute = true;

            Env<AttrContext> result = tree instanceof JCExpression ?
                    attribExprToTree((JCExpression) tree, env, (JCTree) to) :
                    attribStatToTree((JCTree) tree, env, (JCTree) to);

            return fullyAttributeResult != null ? fullyAttributeResult : result;
        } finally {
            fullyAttribute = false;
        }
    }
}
