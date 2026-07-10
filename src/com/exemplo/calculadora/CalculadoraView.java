package com.exemplo.calculadora;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Stack;

public class CalculadoraView extends LinearLayout {

    // Caracteres Unicode para evitar problemas de encoding
    private static final String DIVISAO = "\u00F7"; // ÷
    private static final String MULTIPLIC = "\u00D7"; // ×
    private static final String SUBTRACAO = "\u2212"; // −
    private static final String MAIS = "+";
    private static final String IGUAL = "=";
    private static final String LIMPAR = "C";
    private static final String BACKSPACE = "\u232B"; // ⌫
    private static final String VIRGULA = ".";
    private static final String MAISMENOS = "\u00B1"; // ±
    private static final String PORCENT = "%";

    private TextView display;
    private StringBuilder expressao = new StringBuilder();
    private boolean resetDisplay = false;

    public CalculadoraView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        // dentro do construtor, logo após setOrientation(VERTICAL);
        setBackground(null); // transparente → mostra o gradiente do pai
        // LayoutParams: ocupar toda a largura e altura disponíveis
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // === DISPLAY ===
        display = new TextView(context);
        display.setTextSize(36);
        display.setTextColor(Color.WHITE);
        display.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        display.setPadding(24, 48, 24, 24);
        display.setText("0");
        display.setTypeface(Typeface.MONOSPACE);
        LayoutParams displayParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0);
        displayParams.weight = 1; // 1 parte do espaço vertical
        addView(display, displayParams);

        // === GRADE DE BOTÕES ===
        GridLayout botoes = new GridLayout(context);
        botoes.setColumnCount(4);
        botoes.setRowCount(5);
        LayoutParams gridParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0);
        gridParams.weight = 2; // 2 partes → ocupa 2/3 da altura
        botoes.setLayoutParams(gridParams);

        String[][] teclas = {
                { LIMPAR, MAISMENOS, PORCENT, DIVISAO },
                { "7", "8", "9", MULTIPLIC },
                { "4", "5", "6", SUBTRACAO },
                { "1", "2", "3", MAIS },
                { "0", VIRGULA, BACKSPACE, IGUAL }
        };

        for (int r = 0; r < teclas.length; r++) {
            for (int c = 0; c < teclas[r].length; c++) {
                Button btn = criarBotao(teclas[r][c]);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.rowSpec = GridLayout.spec(r, 1f);
                params.columnSpec = GridLayout.spec(c, 1f);
                params.width = 0;
                params.height = 0;
                // Margens pequenas para separação visual
                int margin = dpToPx(4);
                params.setMargins(margin, margin, margin, margin);
                botoes.addView(btn, params);
            }
        }

        addView(botoes);
    }

    private Button criarBotao(String texto) {
        Button btn = new Button(getContext());
        btn.setText(texto);
        btn.setTextSize(22);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setAllCaps(false);
        btn.setBackgroundColor(Color.parseColor("#16213E"));
        btn.setTextColor(Color.WHITE);
        btn.setPadding(8, 8, 8, 8);
        btn.setOnClickListener(this::onClickBotao);

        if (texto.equals(IGUAL)) {
            btn.setBackgroundColor(Color.parseColor("#E94560"));
        } else if (texto.equals(DIVISAO) || texto.equals(MULTIPLIC) ||
                texto.equals(SUBTRACAO) || texto.equals(MAIS) ||
                texto.equals(LIMPAR)) {
            btn.setBackgroundColor(Color.parseColor("#0F3460"));
        }
        return btn;
    }

    public void onClickBotao(View v) {
        Button btn = (Button) v;
        String valor = btn.getText().toString();

        if (valor.equals(LIMPAR)) {
            expressao.setLength(0);
            display.setText("0");
            resetDisplay = false;
            return;
        }
        if (valor.equals(BACKSPACE)) {
            if (expressao.length() > 0) {
                expressao.deleteCharAt(expressao.length() - 1);
                if (expressao.length() == 0)
                    display.setText("0");
                else
                    display.setText(expressao.toString());
            }
            return;
        }
        if (valor.equals(IGUAL)) {
            calcularResultado();
            resetDisplay = true;
            return;
        }
        if (valor.equals(MAISMENOS)) {
            if (expressao.length() > 0) {
                try {
                    double d = Double.parseDouble(expressao.toString());
                    d = -d;
                    expressao.setLength(0);
                    expressao.append(formatarNumero(d));
                    display.setText(expressao.toString());
                } catch (NumberFormatException ignored) {
                }
            }
            return;
        }

        if (resetDisplay) {
            expressao.setLength(0);
            resetDisplay = false;
        }

        // Substitui caracteres especiais antes de mostrar
        String paraMostrar = valor;
        if (valor.equals(DIVISAO))
            paraMostrar = DIVISAO;
        else if (valor.equals(MULTIPLIC))
            paraMostrar = MULTIPLIC;
        else if (valor.equals(SUBTRACAO))
            paraMostrar = SUBTRACAO;

        expressao.append(paraMostrar);
        display.setText(expressao.toString());
    }

    private String formatarNumero(double valor) {
        if (valor == (long) valor)
            return String.valueOf((long) valor);
        else
            return String.valueOf(valor);
    }

    private void calcularResultado() {
        if (expressao.length() == 0)
            return;
        try {
            double resultado = avaliarExpressao(expressao.toString()
                    .replace(DIVISAO, "/")
                    .replace(MULTIPLIC, "*")
                    .replace(SUBTRACAO, "-"));
            String res = formatarNumero(resultado);
            display.setText(res);
            expressao.setLength(0);
            expressao.append(res);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Erro na expressão", Toast.LENGTH_SHORT).show();
            display.setText("Erro");
            expressao.setLength(0);
        }
    }

    private double avaliarExpressao(String expr) {
        expr = expr.replaceAll("\\s+", "");
        if (expr.isEmpty())
            return 0;
        Stack<Double> numeros = new Stack<>();
        Stack<Character> ops = new Stack<>();
        int i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (Character.isDigit(c) || c == '.') {
                StringBuilder sb = new StringBuilder();
                while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                    sb.append(expr.charAt(i));
                    i++;
                }
                numeros.push(Double.parseDouble(sb.toString()));
                continue;
            } else if (c == '+' || c == '-' || c == '*' || c == '/') {
                while (!ops.isEmpty() && precedencia(ops.peek()) >= precedencia(c)) {
                    aplicarOperacao(numeros, ops.pop());
                }
                ops.push(c);
            }
            i++;
        }
        while (!ops.isEmpty()) {
            aplicarOperacao(numeros, ops.pop());
        }
        return numeros.pop();
    }

    private int precedencia(char op) {
        if (op == '+' || op == '-')
            return 1;
        if (op == '*' || op == '/')
            return 2;
        return 0;
    }

    private void aplicarOperacao(Stack<Double> nums, char op) {
        if (nums.size() < 2)
            return;
        double b = nums.pop();
        double a = nums.pop();
        switch (op) {
            case '+':
                nums.push(a + b);
                break;
            case '-':
                nums.push(a - b);
                break;
            case '*':
                nums.push(a * b);
                break;
            case '/':
                nums.push(a / b);
                break;
        }
    }

    // Converte dp para pixels (útil para margens adaptáveis)
    private int dpToPx(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}