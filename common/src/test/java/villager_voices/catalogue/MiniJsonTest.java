package villager_voices.catalogue;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MiniJsonTest {

    @Test
    void parsesTheCatalogueShape() {
        Object parsed = MiniJson.parse("""
            {
              "lines": [
                { "subtitle": "Mrrgh -- traded! Nice.", "sound": "villager_voices:reaction.trade_completed.1" }
              ]
            }
            """);
        Map<?, ?> root = (Map<?, ?>) parsed;
        List<?> lines = (List<?>) root.get("lines");
        Map<?, ?> line = (Map<?, ?>) lines.get(0);
        assertEquals("Mrrgh -- traded! Nice.", line.get("subtitle"));
        assertEquals("villager_voices:reaction.trade_completed.1", line.get("sound"));
    }

    @Test
    void unescapesStandardEscapesAndUnicode() {
        Object parsed = MiniJson.parse("\"a\\\"b\\\\c\\/d\\n\\t\\u0041\"");
        assertEquals("a\"b\\c/d\n\tA", parsed);
    }

    @Test
    void parsesEmptyObjectsAndArrays() {
        assertEquals(Map.of(), MiniJson.parse("{}"));
        assertEquals(List.of(), MiniJson.parse("[]"));
    }

    @Test
    void parsesBooleansAndNull() {
        assertEquals(Boolean.TRUE, MiniJson.parse("true"));
        assertEquals(Boolean.FALSE, MiniJson.parse("false"));
        assertNull(MiniJson.parse("null"));
    }

    @Test
    void parsesNumbers() {
        assertEquals(4.0, MiniJson.parse("4"));
        assertEquals(-1.5, MiniJson.parse("-1.5"));
        assertEquals(2.5e3, MiniJson.parse("2.5e3"));
    }

    @Test
    void rejectsMalformedJson() {
        assertThrows(MiniJson.JsonSyntaxException.class, () -> MiniJson.parse("{"));
        assertThrows(MiniJson.JsonSyntaxException.class, () -> MiniJson.parse("{\"a\": }"));
        assertThrows(MiniJson.JsonSyntaxException.class, () -> MiniJson.parse("not json"));
    }

    @Test
    void rejectsTrailingContent() {
        assertThrows(MiniJson.JsonSyntaxException.class, () -> MiniJson.parse("{} garbage"));
    }
}
