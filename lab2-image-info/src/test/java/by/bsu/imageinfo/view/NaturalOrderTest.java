package by.bsu.imageinfo.view;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NaturalOrderTest {

    @Test
    void numbersAreComparedByValue() {
        List<String> names = new ArrayList<>(List.of("img10.jpg", "img2.jpg", "IMG1.jpg", "img02b.jpg", "a.jpg"));
        names.sort(NaturalOrder.INSTANCE);
        assertEquals(List.of("a.jpg", "IMG1.jpg", "img2.jpg", "img02b.jpg", "img10.jpg"), names);
    }
}
