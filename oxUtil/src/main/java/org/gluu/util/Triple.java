/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.util;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 26/11/2012
 */

public class Triple<A, B, C> {

    private A first;
    private B second;
    private C third;

    public Triple() {
    }

    public Triple(A first, B second, C third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public A getFirst() {
        return first;
    }

    public void setFirst(A first) {
        this.first = first;
    }

    public B getSecond() {
        return second;
    }

    public void setSecond(B second) {
        this.second = second;
    }

    public C getThird() {
        return third;
    }

    public void setThird(C third) {
        this.third = third;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Triple triple = (Triple) o;

        if (first != null ? !first.equals(triple.first) : triple.first != null) {
            return false;
        }
        if (second != null ? !second.equals(triple.second) : triple.second != null) {
            return false;
        }
        if (third != null ? !third.equals(triple.third) : triple.third != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = first != null ? first.hashCode() : 0;
        result = 31 * result + (second != null ? second.hashCode() : 0);
        result = 31 * result + (third != null ? third.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Triple");
        sb.append("{first=").append(first);
        sb.append(", second=").append(second);
        sb.append(", third=").append(third);
        sb.append('}');
        return sb.toString();
    }
}
