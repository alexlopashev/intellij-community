package com.jetbrains.python.codeInsight.editorActions.moveUpDown;

import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.codeInsight.editorActions.moveUpDown.LineMover;
import com.intellij.codeInsight.editorActions.moveUpDown.LineRange;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiDocumentManagerImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.psi.*;
import com.jetbrains.python.psi.impl.PyPsiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * Author: Alexey.Ivanov
 * Date:   23.04.2010
 * Time:   16:44:57
 */
public class StatementMover extends LineMover {
  private static final Logger LOG = Logger.getInstance("#com.jetbrains.python.codeInsight.editorActions.moveUpDown.StatementMover");

  private static PsiElement[] findStatementsInRange(PsiFile file, int startOffset, int endOffset) {
    PsiElement element1 = file.findElementAt(startOffset);
    PsiElement element2 = file.findElementAt(endOffset - 1);
    if (element1 instanceof PsiWhiteSpace) {
      startOffset = element1.getTextRange().getEndOffset();
      element1 = file.findElementAt(startOffset);
    }
    if (element2 instanceof PsiWhiteSpace) {
      endOffset = element2.getTextRange().getStartOffset();
      element2 = file.findElementAt(endOffset - 1);
    }
    if (element1 == null || element2 == null) {
      return PsiElement.EMPTY_ARRAY;
    }

    PsiElement parent = PsiTreeUtil.findCommonParent(element1, element2);
    if (parent == null) {
      return PsiElement.EMPTY_ARRAY;
    }

    while (true) {
      if (parent instanceof PyStatement) {
        parent = parent.getParent();
        break;
      }
      if (parent instanceof PyStatementList) {
        break;
      }
      if (parent == null || parent instanceof PsiFile) {
        return PsiElement.EMPTY_ARRAY;
      }
      parent = parent.getParent();
    }

    if (!parent.equals(element1)) {
      while (!parent.equals(element1.getParent())) {
        element1 = element1.getParent();
      }
    }

    if (!parent.equals(element2)) {
      while (!parent.equals(element2.getParent())) {
        element2 = element2.getParent();
      }
    }

    PsiElement[] children = parent.getChildren();
    ArrayList<PsiElement> array = new ArrayList<PsiElement>();
    boolean flag = false;
    for (PsiElement child : children) {
      if (child.equals(element1)) {
        flag = true;
      }
      if (flag && !(child instanceof PsiWhiteSpace)) {
        array.add(child);
      }
      if (child.equals(element2)) {
        break;
      }
    }

    for (PsiElement element : array) {
      if (!(element instanceof PyStatement || element instanceof PsiWhiteSpace || element instanceof PsiComment)) {
        return PsiElement.EMPTY_ARRAY;
      }
    }
    return array.toArray(new PsiElement[array.size()]);
  }

  private static boolean isNotValidStatementRange(Pair<PsiElement, PsiElement> range) {
    return range == null ||
           range.first == null ||
           range.second == null ||
           range.first.getParent() != range.second.getParent();
  }

  @Nullable
  private static LineRange expandLineRange(LineRange range, Editor editor, PsiFile file) {
    final SelectionModel selectionModel = editor.getSelectionModel();
    Pair<PsiElement, PsiElement> psiRange;
    if (selectionModel.hasSelection()) {
      final int startOffset = selectionModel.getSelectionStart();
      final int endOffset = selectionModel.getSelectionEnd();
      final PsiElement[] psiElements = findStatementsInRange(file, startOffset, endOffset);
      if (psiElements.length == 0) {
        return null;
      }
      psiRange = new Pair<PsiElement, PsiElement>(psiElements[0], psiElements[psiElements.length - 1]);
    }
    else {
      final int offset = editor.getCaretModel().getOffset();
      PsiElement element = file.findElementAt(offset);
      psiRange = new Pair<PsiElement, PsiElement>(element, element);
    }

    psiRange = new Pair<PsiElement, PsiElement>(PsiTreeUtil.getNonStrictParentOfType(psiRange.getFirst(), PyStatement.class),
                                                PsiTreeUtil.getNonStrictParentOfType(psiRange.getSecond(), PyStatement.class));
    if (psiRange.getFirst() == null || psiRange.getSecond() == null) {
      return null;
    }

    final PsiElement parent = PsiTreeUtil.findCommonParent(psiRange.getFirst(), psiRange.getSecond());
    final Pair<PsiElement, PsiElement> elementRange = getElementRange(parent, psiRange.getFirst(), psiRange.getSecond());
    if (isNotValidStatementRange(elementRange)) {
      return null;
    }

    if (elementRange.getFirst() == elementRange.getSecond() && elementRange.getFirst() instanceof PyPassStatement) {
      return null;
    }

    final int endOffset = elementRange.getSecond().getTextRange().getEndOffset();
    final Document document = editor.getDocument();
    if (endOffset > document.getTextLength()) {
      LOG.assertTrue(!PsiDocumentManager.getInstance(file.getProject()).isUncommited(document));
      LOG.assertTrue(PsiDocumentManagerImpl.checkConsistency(file, document));
    }

    int endLine;
    if (endOffset == document.getTextLength()) {
      endLine = document.getLineCount();
    }
    else {
      endLine = editor.offsetToLogicalPosition(endOffset).line + 1;
      endLine = Math.min(endLine, document.getLineCount());
    }
    endLine = Math.max(endLine, range.endLine);
    final int startLine = Math.min(range.startLine, editor.offsetToLogicalPosition(elementRange.getFirst().getTextOffset()).line);
    return new LineRange(startLine, endLine);
  }

  private @Nullable PyStatementList myStatementListToAddPass;
  private @Nullable PyStatementList myStatementListToRemovePass;
  private @NotNull PsiElement[] myElementsToDecreaseIndent;
  private @NotNull PsiElement[] myElementsToIncreaseIndent;

  @Override
  public boolean checkAvailable(@NotNull Editor editor,
                                @NotNull PsiFile file,
                                @NotNull MoveInfo info,
                                boolean down) {
    myStatementListToAddPass = null;
    myStatementListToRemovePass = null;
    myElementsToDecreaseIndent = PsiElement.EMPTY_ARRAY;
    myElementsToIncreaseIndent = PsiElement.EMPTY_ARRAY;
    if (!(file instanceof PyFile)) {
      return false;
    }
    if (!super.checkAvailable(editor, file, info, down)) {
      return false;
    }
    info.indentSource = true;
    final LineRange range = expandLineRange(info.toMove, editor, file);
    if (range == null) {
      return false;
    }

    info.toMove = range;
    final int startOffset = editor.logicalPositionToOffset(new LogicalPosition(range.startLine, 0));
    final int endOffset = editor.logicalPositionToOffset(new LogicalPosition(range.endLine, 0));
    final PsiElement[] statements = findStatementsInRange(file, startOffset, endOffset);
    final int length = statements.length;
    if (length == 0) {
      return false;
    }

    range.firstElement = statements[0];
    range.lastElement = statements[length - 1];
    final Document document = editor.getDocument();
    PyStatement statement;
    assert info.toMove2.startLine + 1 == info.toMove2.endLine;
    if (down) {
      statement = PsiTreeUtil.getNextSiblingOfType(range.lastElement, PyStatement.class);
    }
    else {
      statement = PsiTreeUtil.getPrevSiblingOfType(range.firstElement, PyStatement.class);
    }

    if (statement == null) {
      final PyStatementPart parentStatementPart =
        PsiTreeUtil.getParentOfType(PsiTreeUtil.findCommonParent(range.firstElement, range.lastElement), PyStatementPart.class, false);
      if (parentStatementPart == null) {
        info.toMove2 = null;
      }
      else {  //we are in statement part

        final PyStatementList statementList = parentStatementPart.getStatementList();
        assert statementList != null;
        if (down) {
          final PyStatementPart nextStatementPart = PsiTreeUtil.getNextSiblingOfType(statementList.getParent(), PyStatementPart.class);
          if (nextStatementPart != null) {
            info.toMove2 = new LineRange(range.endLine, range.endLine + 1);
            myStatementListToRemovePass = nextStatementPart.getStatementList();
          }
          else {
            final PyStatement parentStatement = PsiTreeUtil.getParentOfType(statementList, PyStatement.class);
            if (parentStatement == null) {
              return false;
            }
            final PyStatement nextStatement = PsiTreeUtil.getNextSiblingOfType(parentStatement, PyStatement.class);
            if (nextStatement == null) {
              return false;
            }
            final int startLine = editor.offsetToLogicalPosition(parentStatement.getTextRange().getEndOffset()).line;
            final int endLine = editor.offsetToLogicalPosition(nextStatement.getTextRange().getEndOffset()).line;
            info.toMove2 = new LineRange(startLine + 1, endLine + 1);

            myElementsToDecreaseIndent = statements;
          }
        }
        else {
          final PyStatementPart prevStatementPart = PsiTreeUtil.getPrevSiblingOfType(statementList.getParent(), PyStatementPart.class);
          if (prevStatementPart != null) {
            myStatementListToRemovePass = prevStatementPart.getStatementList();
          }
          else {
            myElementsToDecreaseIndent = statements;
            info.toMove2 = new LineRange(range.startLine - 1, range.startLine);
          }
        }
        if (Arrays.equals(statementList.getStatements(), statements)) {
          myStatementListToAddPass = statementList;
        }
      }
      return true;
    }

    info.toMove2 = new LineRange(statement, statement, document);

    final PyStatementPart[] statementParts = PsiTreeUtil.getChildrenOfType(statement, PyStatementPart.class);
    // next/previous statement has a statement parts
    if (statementParts != null) {
      // move inside statement part
      if (down) {
        final PyStatementPart statementPart = statementParts[0];
        final int lineNumber = document.getLineNumber(statementPart.getTextRange().getStartOffset());
        info.toMove2 = new LineRange(lineNumber, lineNumber + 1);
        myStatementListToRemovePass = statementPart.getStatementList();
        myElementsToIncreaseIndent = statements;
      }
      else {
        final PyStatementPart statementPart = statementParts[statementParts.length - 1];
        final int lineNumber = document.getLineNumber(statementPart.getTextRange().getEndOffset());
        info.toMove2 = new LineRange(lineNumber, lineNumber + 1);
        myStatementListToRemovePass = statementPart.getStatementList();
        myElementsToIncreaseIndent = statements;
      }
    }
    return true;
  }

  private void decreaseIndent(final Editor editor) {
    final Document document = editor.getDocument();
    for (PsiElement statement : myElementsToDecreaseIndent) {
      final int startOffset = statement.getTextRange().getStartOffset() - 1;
      PsiElement element = statement.getContainingFile().findElementAt(startOffset);
      assert element instanceof PsiWhiteSpace;
      final String text = element.getText();
      String[] lines = text.split("\n");
      if (lines.length == 0) {
        continue;
      }
      final int indent = lines[lines.length - 1].length();
      final int startLine = editor.offsetToLogicalPosition(startOffset).line;
      final int endLine = editor.offsetToLogicalPosition(statement.getTextRange().getEndOffset()).line;
      for (int line = startLine; line <= endLine; ++line) {
        if (indent >= 4) {
          final int lineStartOffset = document.getLineStartOffset(line);
          document.deleteString(lineStartOffset, lineStartOffset + 4);
        }
      }
    }
  }

  private void increaseIndent(final Editor editor) {
    final Document document = editor.getDocument();
    for (PsiElement statement : myElementsToIncreaseIndent) {
      final int startLine = editor.offsetToLogicalPosition(statement.getTextRange().getStartOffset()).line;
      final int endLine = editor.offsetToLogicalPosition(statement.getTextRange().getEndOffset()).line;
      for (int line = startLine; line <= endLine; ++line) {
        final int offset = document.getLineStartOffset(line);
        document.insertString(offset, "    ");
      }
    }
  }

  @Override
  public void beforeMove(@NotNull Editor editor, @NotNull MoveInfo info, boolean down) {
    super.beforeMove(editor, info, down);
    if (myStatementListToAddPass != null) {
      final PyPassStatement passStatement =
        PyElementGenerator.getInstance(editor.getProject()).createFromText(PyPassStatement.class, "pass");
      myStatementListToAddPass.add(passStatement);
      CodeInsightUtilBase.forcePsiPostprocessAndRestoreElement(myStatementListToAddPass);
      if (down) {
        info.toMove2 = new LineRange(info.toMove2.startLine, info.toMove2.endLine + 1);
      }
    }
    decreaseIndent(editor);
    increaseIndent(editor);
  }

  @Override
  public void afterMove(@NotNull Editor editor,
                        @NotNull PsiFile file,
                        @NotNull MoveInfo info,
                        boolean down) {
    super.afterMove(editor, file, info, down);
    if (myStatementListToRemovePass != null) {
      PyPsiUtils.removeRedundantPass(myStatementListToRemovePass);
      CodeInsightUtilBase.forcePsiPostprocessAndRestoreElement(myStatementListToRemovePass);
    }
  }
}
