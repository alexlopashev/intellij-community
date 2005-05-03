package com.siyeh.ig.j2me;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.psi.*;
import com.siyeh.ig.*;

public class ArrayLengthInLoopConditionInspection extends StatementInspection {

    public String getDisplayName() {
        return "Array.length in loop condition";
    }

    public String getGroupDisplayName() {
        return GroupNames.J2ME_GROUP_NAME;
    }

    public String buildErrorString(PsiElement location) {
        return "Check of array .#ref in loop condition #loc";
    }

    public BaseInspectionVisitor createVisitor(InspectionManager inspectionManager, boolean onTheFly) {
        return new ArrayLengthInLoopConditionVisitor(this, inspectionManager, onTheFly);
    }

    private static class ArrayLengthInLoopConditionVisitor extends StatementInspectionVisitor {
        private ArrayLengthInLoopConditionVisitor(BaseInspection inspection, InspectionManager inspectionManager, boolean isOnTheFly) {
            super(inspection, inspectionManager, isOnTheFly);
        }

        public void visitForStatement(PsiForStatement statement) {
            super.visitForStatement(statement);
            final PsiExpression condition = statement.getCondition();
            if(condition== null)
            {
                return;
            }
            checkForMethodCalls(condition);
        }

        public void visitWhileStatement(PsiWhileStatement statement) {
            super.visitWhileStatement(statement);
            final PsiExpression condition = statement.getCondition();
            if(condition == null){
                return;
            }
            checkForMethodCalls(condition);
        }


        public void visitDoWhileStatement(PsiDoWhileStatement statement) {
            super.visitDoWhileStatement(statement);
            final PsiExpression condition = statement.getCondition();
            if(condition == null){
                return;
            }
            checkForMethodCalls(condition);
        }

        private void checkForMethodCalls(PsiExpression condition){
            final PsiRecursiveElementVisitor visitor = new PsiRecursiveElementVisitor(){
                public void visitReferenceExpression(PsiReferenceExpression expression){
                    super.visitReferenceExpression(expression);
                    final String name = expression.getReferenceName();
                    if(!"length".equals(name))
                    {
                        return;
                    }
                    final PsiExpression qualifier = expression.getQualifierExpression();
                    if(qualifier == null)
                    {
                        return;
                    }
                    final PsiType type = qualifier.getType();
                    if(!(type instanceof PsiArrayType))
                    {
                        return;
                    }
                    final PsiElement lengthElement = expression.getReferenceNameElement();
                    registerError(lengthElement);
                }
            };
            condition.accept(visitor);
        }

    }

}
