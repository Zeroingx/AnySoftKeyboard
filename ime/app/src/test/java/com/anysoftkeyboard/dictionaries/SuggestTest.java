package com.anysoftkeyboard.dictionaries;

import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class SuggestTest {

    private SuggestionsProvider mProvider;
    private Suggest mUnderTest;

    private static void typeWord(WordComposer wordComposer, String word) {
        for (int charIndex = 0; charIndex < word.length(); charIndex++) {
            final char c = word.charAt(charIndex);
            wordComposer.add(c, new int[] {c});
        }
    }

    @Before
    public void setUp() throws Exception {
        mProvider = Mockito.mock(SuggestionsProvider.class);
        mUnderTest = new SuggestImpl(mProvider);
    }

    @Test
    public void testDelegatesIncognito() {
        Mockito.verify(mProvider, Mockito.never()).setIncognitoMode(Mockito.anyBoolean());

        mUnderTest.setIncognitoMode(true);
        Mockito.doReturn(true).when(mProvider).isIncognitoMode();

        Mockito.verify(mProvider).setIncognitoMode(true);
        Mockito.verifyNoMoreInteractions(mProvider);

        Assert.assertTrue(mUnderTest.isIncognitoMode());
        Mockito.verify(mProvider).isIncognitoMode();
        Mockito.verifyNoMoreInteractions(mProvider);
        Mockito.reset(mProvider);

        mUnderTest.setIncognitoMode(false);
        Mockito.doReturn(false).when(mProvider).isIncognitoMode();

        Mockito.verify(mProvider).setIncognitoMode(false);
        Mockito.verifyNoMoreInteractions(mProvider);

        Assert.assertFalse(mUnderTest.isIncognitoMode());
        Mockito.verify(mProvider).isIncognitoMode();
        Mockito.verifyNoMoreInteractions(mProvider);
    }

    @Test
    public void testHasCorrectionWhenHaveCommonalitySuggestions() {
        mUnderTest.setCorrectionMode(true, 1, 2);
        WordComposer wordComposer = new WordComposer();
        Mockito.doAnswer(
                        invocation -> {
                            final Dictionary.WordCallback callback = invocation.getArgument(1);
                            callback.addWord(
                                    "hello".toCharArray(),
                                    0,
                                    5,
                                    23,
                                    Mockito.mock(Dictionary.class));
                            return null;
                        })
                .when(mProvider)
                .getSuggestions(Mockito.any(), Mockito.any());

        // since we asked for 2 minimum-length, the first letter will not be queried
        typeWord(wordComposer, "hel");
        Assert.assertEquals(2, mUnderTest.getSuggestions(wordComposer).size());
        // no close correction
        Assert.assertEquals(-1, mUnderTest.getLastValidSuggestionIndex());
        typeWord(wordComposer, "l");
        Assert.assertEquals(2, mUnderTest.getSuggestions(wordComposer).size());
        // we have a close correction for you at index 1
        Assert.assertEquals(1, mUnderTest.getLastValidSuggestionIndex());
        typeWord(wordComposer, "o");
        // the same word typed as received from the dictionary, so pruned.
        Assert.assertEquals(1, mUnderTest.getSuggestions(wordComposer).size());
        // the typed word is valid and is at index 0
        Assert.assertEquals(0, mUnderTest.getLastValidSuggestionIndex());
    }

    @Test
    public void testNeverQueriesWhenSuggestionsOff() {
        mUnderTest.setCorrectionMode(false, 5, 2);
        WordComposer wordComposer = new WordComposer();
        typeWord(wordComposer, "hello");
        final List<CharSequence> suggestions = mUnderTest.getSuggestions(wordComposer);
        Assert.assertTrue(suggestions.isEmpty());
        Mockito.verifyZeroInteractions(mProvider);
        Assert.assertEquals(-1, mUnderTest.getLastValidSuggestionIndex());
    }

    @Test
    public void testQueriesWhenSuggestionsOn() {
        mUnderTest.setCorrectionMode(true, 5, 2);
        WordComposer wordComposer = new WordComposer();
        Mockito.doAnswer(
                        invocation -> {
                            final WordComposer word = invocation.getArgument(0);
                            if (word.codePointCount() > 1) {
                                final Dictionary.WordCallback callback = invocation.getArgument(1);
                                callback.addWord(
                                        "hello".toCharArray(),
                                        0,
                                        5,
                                        23,
                                        Mockito.mock(Dictionary.class));
                            }
                            return null;
                        })
                .when(mProvider)
                .getSuggestions(Mockito.any(), Mockito.any());

        // since we asked for 2 minimum-length, the first letter will not be queried
        typeWord(wordComposer, "h");
        final List<CharSequence> suggestions1 = mUnderTest.getSuggestions(wordComposer);
        Assert.assertEquals(1, suggestions1.size());
        Assert.assertEquals("h", suggestions1.get(0));
        Mockito.verify(mProvider).getSuggestions(Mockito.any(), Mockito.any());
        typeWord(wordComposer, "e");
        final List<CharSequence> suggestions2 = mUnderTest.getSuggestions(wordComposer);
        Assert.assertEquals(2, suggestions2.size());
        Assert.assertEquals("he", suggestions2.get(0).toString());
        Assert.assertEquals("hello", suggestions2.get(1).toString());
        Mockito.verify(mProvider, Mockito.times(2))
                .getSuggestions(Mockito.same(wordComposer), Mockito.any());
        Assert.assertSame(suggestions1, suggestions2);
    }

    @Test
    public void testHasCorrectionWhenHaveAbbreviation() {
        mUnderTest.setCorrectionMode(true, 5, 2);
        WordComposer wordComposer = new WordComposer();
        Mockito.doAnswer(
                        invocation -> {
                            final WordComposer word = invocation.getArgument(0);
                            final Dictionary.WordCallback callback = invocation.getArgument(1);
                            if (word.getTypedWord().equals("wfh")) {
                                callback.addWord(
                                        "work from home".toCharArray(),
                                        0,
                                        14,
                                        23,
                                        Mockito.mock(Dictionary.class));
                            }
                            return null;
                        })
                .when(mProvider)
                .getAbbreviations(Mockito.any(), Mockito.any());

        typeWord(wordComposer, "w");
        Assert.assertEquals(1, mUnderTest.getSuggestions(wordComposer).size());
        Mockito.verify(mProvider).getAbbreviations(Mockito.same(wordComposer), Mockito.any());
        Assert.assertEquals(-1, mUnderTest.getLastValidSuggestionIndex());

        // this is the second letter, it should be queried.
        typeWord(wordComposer, "f");
        final List<CharSequence> suggestions1 = mUnderTest.getSuggestions(wordComposer);
        Assert.assertEquals(1, suggestions1.size());
        Assert.assertEquals("wf", suggestions1.get(0));
        Mockito.verify(mProvider, Mockito.times(2))
                .getAbbreviations(Mockito.same(wordComposer), Mockito.any());
        Assert.assertEquals(-1, mUnderTest.getLastValidSuggestionIndex());
        typeWord(wordComposer, "h");
        final List<CharSequence> suggestions2 = mUnderTest.getSuggestions(wordComposer);
        Assert.assertEquals(2, suggestions2.size());
        Assert.assertEquals("wfh", suggestions2.get(0).toString());
        Assert.assertEquals("work from home", suggestions2.get(1).toString());
        Mockito.verify(mProvider, Mockito.times(3))
                .getAbbreviations(Mockito.same(wordComposer), Mockito.any());
        Assert.assertSame(suggestions1, suggestions2);
        Assert.assertEquals(1, mUnderTest.getLastValidSuggestionIndex());
    }

    @Test
    public void testAbbreviationsOverTakeDictionarySuggestions() {
        mUnderTest.setCorrectionMode(true, 5, 2);
        WordComposer wordComposer = new WordComposer();
        Mockito.doAnswer(
                        invocation -> {
                            final WordComposer word = invocation.getArgument(0);
                            final Dictionary.WordCallback callback = invocation.getArgument(1);
                            if (word.getTypedWord().equals("hate")) {
                                callback.addWord(
                                        "love".toCharArray(),
                                        0,
                                        4,
                                        23,
                                        Mockito.mock(Dictionary.class));
                            }
                            return null;
                        })
                .when(mProvider)
                .getAbbreviations(Mockito.any(), Mockito.any());
        Mockito.doAnswer(
                        invocation -> {
                            final Dictionary.WordCallback callback = invocation.getArgument(1);
                            callback.addWord(
                                    "hate".toCharArray(), 0, 4, 23, Mockito.mock(Dictionary.class));
                            return null;
                        })
                .when(mProvider)
                .getSuggestions(Mockito.any(), Mockito.any());

        typeWord(wordComposer, "hat");
        final InOrder inOrder = Mockito.inOrder(mProvider);
        final List<CharSequence> suggestions1 = mUnderTest.getSuggestions(wordComposer);
        Assert.assertEquals(2, suggestions1.size());
        Assert.assertEquals("hat", suggestions1.get(0).toString());
        Assert.assertEquals("hate", suggestions1.get(1).toString());
        // ensuring abbr are called first, so the max-suggestions will not hide the exploded abbr.
        inOrder.verify(mProvider).getAbbreviations(Mockito.same(wordComposer), Mockito.any());
        inOrder.verify(mProvider).getSuggestions(Mockito.same(wordComposer), Mockito.any());
        // suggesting "hate" as a correction (from dictionary)
        Assert.assertEquals(1, mUnderTest.getLastValidSuggestionIndex());

        // hate should be converted to love
        typeWord(wordComposer, "e");
        final List<CharSequence> suggestions2 = mUnderTest.getSuggestions(wordComposer);
        Assert.assertEquals(2, suggestions2.size());
        Assert.assertEquals("hate", suggestions2.get(0).toString());
        Assert.assertEquals("love", suggestions2.get(1).toString());
        inOrder.verify(mProvider).getAbbreviations(Mockito.same(wordComposer), Mockito.any());
        inOrder.verify(mProvider).getSuggestions(Mockito.same(wordComposer), Mockito.any());
        // suggestion "love" as a correction (abbr)
        Assert.assertEquals(1, mUnderTest.getLastValidSuggestionIndex());
    }

    @Test
    public void testAutoTextIsQueriedEvenWithOneLetter() {
        mUnderTest.setCorrectionMode(true, 5, 2);
        WordComposer wordComposer = new WordComposer();
        Mockito.doAnswer(
                        invocation -> {
                            final WordComposer word = invocation.getArgument(0);
                            final Dictionary.WordCallback callback = invocation.getArgument(1);
                            if (word.getTypedWord().equals("i")) {
                                callback.addWord("I".toCharArray(), 0, 1, 23, null);
                            }
                            return null;
                        })
                .when(mProvider)
                .getAutoText(Mockito.any(), Mockito.any());

        typeWord(wordComposer, "i");
        List<CharSequence> suggestions = mUnderTest.getSuggestions(wordComposer);
        InOrder inOrder = Mockito.inOrder(mProvider);
        Assert.assertEquals(2, suggestions.size());
        Assert.assertEquals("i", suggestions.get(0).toString());
        Assert.assertEquals("I", suggestions.get(1).toString());
        // ensuring abbr are called first, so the max-suggestions will not hide the exploded abbr.
        inOrder.verify(mProvider).getAbbreviations(Mockito.same(wordComposer), Mockito.any());
        inOrder.verify(mProvider).getAutoText(Mockito.same(wordComposer), Mockito.any());
        inOrder.verify(mProvider).getSuggestions(Mockito.same(wordComposer), Mockito.any());
        // suggesting "I" as a correction (from dictionary)
        Assert.assertEquals(1, mUnderTest.getLastValidSuggestionIndex());

        typeWord(wordComposer, "ll");
        suggestions = mUnderTest.getSuggestions(wordComposer);
        inOrder = Mockito.inOrder(mProvider);
        Assert.assertEquals(1, suggestions.size());
        Assert.assertEquals("ill", suggestions.get(0).toString());
        inOrder.verify(mProvider).getAbbreviations(Mockito.same(wordComposer), Mockito.any());
        inOrder.verify(mProvider).getAutoText(Mockito.same(wordComposer), Mockito.any());
        inOrder.verify(mProvider).getSuggestions(Mockito.same(wordComposer), Mockito.any());
        Assert.assertEquals(
                -1 /*ill is not a valid word in the test*/,
                mUnderTest.getLastValidSuggestionIndex());
    }
}
