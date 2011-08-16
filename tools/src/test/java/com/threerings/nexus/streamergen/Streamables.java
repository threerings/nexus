//
// $Id$

package com.threerings.nexus.streamergen;

import com.threerings.nexus.io.Streamable;

/**
 * Defines a bunch of test streamable classes to test the streamer generator.
 */
public class Streamables
{
    public static class SimplePublic implements Streamable {
        public final int value;
        public SimplePublic (int value) {
            this.value = value;
        }
    }

    public static class SimpleProtected implements Streamable {
        protected SimpleProtected (int value) {
            _value = value;
        }
        protected final int _value;
    }

    public static class GenericOne<A> implements Streamable {
        public final A value;
        public GenericOne (A value) {
            this.value = value;
        }
    }

    public static class GenericTwo<A,B> implements Streamable {
        public final A valueA;
        public final B valueB;
        public GenericTwo (A valueA, B valueB) {
            this.valueA = valueA;
            this.valueB = valueB;
        }
    }

    public static class GenericUpperBounded<A extends Number> implements Streamable {
        public final A value;
        public GenericUpperBounded (A value) {
            this.value = value;
        }
    }

//     public static class GenericLowerBounded<A super Number> implements Streamable {
//         public final A value;
//         public GenericLowerBounded (A value) {
//             this.value = value;
//         }
//     }
}
