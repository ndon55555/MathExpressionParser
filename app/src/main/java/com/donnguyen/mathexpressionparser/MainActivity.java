package com.donnguyen.mathexpressionparser;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void calculate(View view) {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow((null == getCurrentFocus()) ? null : getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        EditText etExpression = (EditText) findViewById(R.id.etExpression);
        String inputExpression = etExpression.getText().toString();
        TextView tvFinalAnswer = (TextView) findViewById(R.id.tvFinalAnswer);

        try {
            double finalAnswer = calculate(inputExpression);
            tvFinalAnswer.setText(Double.toString(finalAnswer));
        } catch (Exception e) {
            tvFinalAnswer.setText(e.getMessage());
        }
    }

    private double calculate(String expression) {
        MathExpression mathExpression = new MathExpression(expression);
        return mathExpression.getValue();
    }

    private static class MathExpression {
        private String expression;
        private String[] parsedExpression;

        public MathExpression(String expression) {
            this.expression = expression;
            parseExpression();
        }

        public MathExpression(String[] parsedExpression) {
            expression = null;
            this.parsedExpression = parsedExpression;
        }

        public double getValue() {
            int leftParenthesisIndex = lastIndexOf("(");

            //recursive call if parentheses are still present
            if (leftParenthesisIndex >= 0) {
                int rightParenthesisIndex = indexOf(")", leftParenthesisIndex);
                List<String> simplifiedExpressionElements = new LinkedList<>();

                for (int i = 0; i < leftParenthesisIndex; i++) {
                    simplifiedExpressionElements.add(parsedExpression[i]);
                }

                List<String> subExpressionElements = new LinkedList<>();

                for (int i = leftParenthesisIndex + 1; i < rightParenthesisIndex; i++) {
                    subExpressionElements.add(parsedExpression[i]);
                }

                MathExpression subExpression = new MathExpression(subExpressionElements.toArray(new String[subExpressionElements.size()]));
                double subExpressionValue = subExpression.getValue();
                simplifiedExpressionElements.add(Double.toString(subExpressionValue));

                for (int i = rightParenthesisIndex + 1; i < parsedExpression.length; i++) {
                    simplifiedExpressionElements.add(parsedExpression[i]);
                }

                return (new MathExpression(simplifiedExpressionElements.toArray(new String[simplifiedExpressionElements.size()]))).getValue();
            }

            //if no parentheses present, order of operations is applied and a value is returned
            String[] simplifiedParsedExpression = parsedExpression;
            simplifiedParsedExpression = simplifyExponentiation(simplifiedParsedExpression);
            simplifiedParsedExpression = simplifyMultiplicationAndDivision(simplifiedParsedExpression);
            simplifiedParsedExpression = simplifyAdditionAndSubtraction(simplifiedParsedExpression);

            if (simplifiedParsedExpression.length != 1) {
                throw new RuntimeException("A final answer could not be computed. Check parentheses and operators.");
            }

            return Double.parseDouble(simplifiedParsedExpression[0]);
        }

        private static String[] simplifyExponentiation(String[] parsedExpression) {
            List<String> simplifiedExpressionElements = new LinkedList<>(Arrays.asList(parsedExpression));
            int caretIndex = simplifiedExpressionElements.lastIndexOf("^");

            while (caretIndex >= 0) {
                double leftNum = Double.parseDouble(simplifiedExpressionElements.get(caretIndex - 1));
                double rightNum = Double.parseDouble(simplifiedExpressionElements.get(caretIndex + 1));
                double simplifiedValue = Math.pow(leftNum, rightNum);
                simplifiedExpressionElements.remove(caretIndex + 1);
                simplifiedExpressionElements.remove(caretIndex);
                simplifiedExpressionElements.set(caretIndex - 1, Double.toString(simplifiedValue));
                caretIndex = simplifiedExpressionElements.lastIndexOf("^");
            }

            return simplifiedExpressionElements.toArray(new String[simplifiedExpressionElements.size()]);
        }

        private static String[] simplifyMultiplicationAndDivision(String[] parsedExpression) {
            List<String> simplifiedExpressionElements = new LinkedList<>(Arrays.asList(parsedExpression));
            int operandIndex = getAsteriskOrForwardSlashIndex(simplifiedExpressionElements);

            while (operandIndex >= 0) {
                double leftNum = Double.parseDouble(simplifiedExpressionElements.get(operandIndex - 1));
                double rightNum = Double.parseDouble(simplifiedExpressionElements.get(operandIndex + 1));
                double simplifiedValue;

                if (simplifiedExpressionElements.get(operandIndex).equals("*")) {
                    simplifiedValue = leftNum * rightNum;
                } else {
                    simplifiedValue = leftNum / rightNum;
                }

                simplifiedExpressionElements.remove(operandIndex + 1);
                simplifiedExpressionElements.remove(operandIndex);
                simplifiedExpressionElements.set(operandIndex - 1, Double.toString(simplifiedValue));
                operandIndex = getAsteriskOrForwardSlashIndex(simplifiedExpressionElements);
            }

            return simplifiedExpressionElements.toArray(new String[simplifiedExpressionElements.size()]);
        }

        private static String[] simplifyAdditionAndSubtraction(String[] parsedExpression) {
            List<String> simplifiedExpressionElements = new LinkedList<>(Arrays.asList(parsedExpression));
            int operandIndex = getPlusOrMinusIndex(simplifiedExpressionElements);

            while (operandIndex >= 0) {
                double leftNum = Double.parseDouble(simplifiedExpressionElements.get(operandIndex - 1));
                double rightNum = Double.parseDouble(simplifiedExpressionElements.get(operandIndex + 1));
                double simplifiedValue;

                if (simplifiedExpressionElements.get(operandIndex).equals("+")) {
                    simplifiedValue = leftNum + rightNum;
                } else {
                    simplifiedValue = leftNum - rightNum;
                }

                simplifiedExpressionElements.remove(operandIndex + 1);
                simplifiedExpressionElements.remove(operandIndex);
                simplifiedExpressionElements.set(operandIndex - 1, Double.toString(simplifiedValue));
                operandIndex = getPlusOrMinusIndex(simplifiedExpressionElements);
            }

            return simplifiedExpressionElements.toArray(new String[simplifiedExpressionElements.size()]);
        }

        private static int getAsteriskOrForwardSlashIndex(List<String> list) {
            int i = 0;

            for (String element : list) {
                if (element.equals("*") || element.equals("/")) {
                    return i;
                }

                i++;
            }

            return -1;
        }

        private static int getPlusOrMinusIndex(List<String> list) {
            int i = 0;

            for (String element : list) {
                if (element.equals("+") || element.equals("-")) {
                    return i;
                }

                i++;
            }

            return -1;
        }

        private void parseExpression() {
            final Character[] SPECIAL_CHARACTERS = {'(', ')', '^', '*', '/', '+', '-'};
            final Set<Character> SPECIAL_CHARACTER_SET = new HashSet<>(Arrays.asList(SPECIAL_CHARACTERS));
            List<String> expressionElements = new LinkedList<>();
            String numberBuilder = "";

            for (int i = 0; i < expression.length(); i++) {
                char c = expression.charAt(i);

                if (SPECIAL_CHARACTER_SET.contains(c)) {
                    if (!numberBuilder.equals("")) {
                        expressionElements.add(numberBuilder);
                        numberBuilder = "";
                    }

                    expressionElements.add("" + c);
                } else if ('0' <= c && c <= '9' || c == '.'){
                    numberBuilder += c;
                }
            }

            if (!numberBuilder.equals("")) {
                expressionElements.add(numberBuilder);
            }

            parsedExpression = expressionElements.toArray(new String[expressionElements.size()]);
        }

        private int indexOf(String s) {
            return indexOf(s, 0);
        }

        private int indexOf(String s, int start) {
            for (int i = start; i < parsedExpression.length; i++) {
                if (parsedExpression[i].equals(s)) {
                    return i;
                }
            }

            return -1;
        }

        private int lastIndexOf(String s) {
            return lastIndexOf(s, parsedExpression.length - 1);
        }

        private int lastIndexOf(String s, int start) {
            int i = start;

            while (i >= 0 && !parsedExpression[i].equals(s)) {
                i--;
            }

            return i;
        }

        public String toString() {
            return Arrays.toString(parsedExpression);
        }
    }
}