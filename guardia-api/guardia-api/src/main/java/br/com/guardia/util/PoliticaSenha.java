package br.com.guardia.util;

import java.nio.charset.StandardCharsets;

/** Política apenas para criação de credenciais. Nunca aplicar ao login/senha atual. */
public final class PoliticaSenha {
    private PoliticaSenha() {}
    public static final String DESCRICAO = "Use no mínimo 8 caracteres, com letra, número e caractere especial (como !, @ ou #).";
    public static String erro(String senha) {
        if (senha == null || senha.codePointCount(0, senha.length()) < 8) return DESCRICAO;
        if (senha.getBytes(StandardCharsets.UTF_8).length > 72)
            return "A senha ultrapassa o limite permitido. Reduza seu tamanho.";
        boolean letra = false, numero = false, especial = false;
        for (int i = 0; i < senha.length();) {
            int cp = senha.codePointAt(i); i += Character.charCount(cp);
            int tipo = Character.getType(cp);
            if (Character.isISOControl(cp) || tipo == Character.SURROGATE)
                return "A senha contém um caractere inválido.";
            letra |= Character.isLetter(cp);
            numero |= Character.isDigit(cp);
            especial |= switch (tipo) {
                case Character.CONNECTOR_PUNCTUATION, Character.DASH_PUNCTUATION,
                     Character.START_PUNCTUATION, Character.END_PUNCTUATION,
                     Character.INITIAL_QUOTE_PUNCTUATION, Character.FINAL_QUOTE_PUNCTUATION,
                     Character.OTHER_PUNCTUATION, Character.MATH_SYMBOL,
                     Character.CURRENCY_SYMBOL, Character.MODIFIER_SYMBOL, Character.OTHER_SYMBOL -> true;
                default -> false;
            };
        }
        return letra && numero && especial ? null : DESCRICAO;
    }
}
