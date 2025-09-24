package com.facetorched.schemplacer.util;

import java.util.NoSuchElementException;
import java.util.Objects;

public class FrameNumberIterator {
    public final int start;
    public final Integer end; // nullable
    public final int step;
    public final boolean loop;

    private int current;
    private boolean stopped = false;

    public FrameNumberIterator(Integer start, Integer end, Integer step, Boolean loop) {
        this.start = (start != null) ? start : 0;
        this.end = (end != null && end < 0) ? null : end; // may be null
        this.step = (step != null) ? step : 1;
        this.loop = (loop != null) ? loop : false;
        this.current = this.start;
    }

    public boolean hasNext() {
    	if (stopped) return false;
        if (end == null) return true; // infinite
        if (loop) return true; // will reset
        // inclusive end
        if (step > 0) return current <= end;
        else return current >= end;
    }

    public int next() {
        if (!hasNext()) throw new NoSuchElementException();
        int val = current;
        current += step;
        if (loop && end != null) {
            if ((step > 0 && val >= end) || (step < 0 && val <= end)) {
                reset();
            }
        }
        return val;
    }
    
    public int peek() {
		if (!hasNext()) throw new NoSuchElementException();
		return current;
	}

    public void reset() {
        current = start;
    }
    
    public void stop() {
		stopped = true;
	}
    
    @Override
    public int hashCode() {
		return Objects.hash(start, end, step, loop);
	}
    
    @Override
    public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof FrameNumberIterator other)) return false;
		return start == other.start
			&& Objects.equals(end, other.end)
			&& step == other.step
			&& loop == other.loop;
	}
}
