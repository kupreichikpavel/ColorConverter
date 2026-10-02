package by.bsu.imageinfo.view;

import java.util.Comparator;

/**
 * «Естественный» порядок имён, как в Проводнике: img2 &lt; img10, регистр не важен.
 * Сравнение идёт по символам без создания промежуточных строк — таблица
 * сортирует сотни тысяч строк.
 */
final class NaturalOrder implements Comparator<String> {

    static final NaturalOrder INSTANCE = new NaturalOrder();

    private NaturalOrder() {
    }

    @Override
    public int compare(String a, String b) {
        int i = 0;
        int j = 0;
        int lengthA = a.length();
        int lengthB = b.length();
        while (i < lengthA && j < lengthB) {
            char ca = a.charAt(i);
            char cb = b.charAt(j);
            if (isDigit(ca) && isDigit(cb)) {
                while (i < lengthA - 1 && a.charAt(i) == '0' && isDigit(a.charAt(i + 1))) {
                    i++;
                }
                while (j < lengthB - 1 && b.charAt(j) == '0' && isDigit(b.charAt(j + 1))) {
                    j++;
                }
                int endA = i;
                int endB = j;
                while (endA < lengthA && isDigit(a.charAt(endA))) {
                    endA++;
                }
                while (endB < lengthB && isDigit(b.charAt(endB))) {
                    endB++;
                }
                int digitsA = endA - i;
                int digitsB = endB - j;
                if (digitsA != digitsB) {
                    return digitsA - digitsB;
                }
                for (; i < endA; i++, j++) {
                    int cmp = a.charAt(i) - b.charAt(j);
                    if (cmp != 0) {
                        return cmp;
                    }
                }
            } else {
                if (ca != cb) {
                    int cmp = Character.toLowerCase(ca) - Character.toLowerCase(cb);
                    if (cmp != 0) {
                        return cmp;
                    }
                }
                i++;
                j++;
            }
        }
        return (lengthA - i) - (lengthB - j);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
