package eapli.aisafe.airport.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CountryListTest {

    @Test
    void ensureAllReturnsNonEmptyArray() {
        final String[] all = CountryList.ALL;
        assertNotNull(all);
        assertTrue(all.length > 0);
    }

    @Test
    void ensureContainsExpectedCountries() {
        final String[] all = CountryList.ALL;
        assertTrue(contains(all, "Portugal"));
        assertTrue(contains(all, "Spain"));
        assertTrue(contains(all, "France"));
        assertEquals(25, all.length);
    }

    @Test
    void ensureAllCountriesAreUnique() {
        final String[] all = CountryList.ALL;
        final var set = new java.util.HashSet<String>();
        for (String c : all) {
            assertTrue(set.add(c), "Duplicate country found: " + c);
        }
    }

    @Test
    void ensureAllCountriesAreNonEmpty() {
        final String[] all = CountryList.ALL;
        for (String c : all) {
            assertNotNull(c);
            assertFalse(c.isBlank());
        }
    }

    @Test
    void ensureCountriesAreSortedAlphabetically() {
        final String[] all = CountryList.ALL;
        for (int i = 1; i < all.length; i++) {
            assertTrue(all[i - 1].compareTo(all[i]) <= 0, "Array not sorted at index " + i);
        }
    }

    private static boolean contains(final String[] array, final String value) {
        for (String s : array) {
            if (s.equals(value)) return true;
        }
        return false;
    }
}