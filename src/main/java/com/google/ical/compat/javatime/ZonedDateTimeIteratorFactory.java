// Copyright (C) 2006 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.ical.compat.javatime;

import com.google.ical.iter.RecurrenceIterable;
import com.google.ical.iter.RecurrenceIterator;
import com.google.ical.iter.RecurrenceIteratorFactory;
import com.google.ical.values.DateTimeValueImpl;
import com.google.ical.values.DateValue;
import com.google.ical.values.TimeValue;

import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.TimeZone;

/**
 * A factory for converting RRULEs and RDATEs into <code>Iterator&lt;LocalDate&gt;</code> and
 * <code>Iterable&lt;LocalDate&gt;</code>.
 *
 * @author Martin Miller
 * @version 0.1 2021-03-20
 */
public class ZonedDateTimeIteratorFactory {

    private ZonedDateTimeIteratorFactory() {}

    /**
     * Given a block of RRULE, EXRULE, RDATE, and EXDATE content lines, parse them into a single local date iterator.
     *
     * @param rdata RRULE, EXRULE, RDATE, and EXDATE lines.
     * @param start the first occurrence of the series.
     * @param strict true if any failure to parse should result in a ParseException. False causes bad content lines to be logged and ignored.
     */
    public static ZonedDateTimeIterator createZonedDateTimeIterator(String rdata, ZonedDateTime start, boolean strict) throws ParseException {
        return new RecurrenceIteratorWrapper(RecurrenceIteratorFactory
            .createRecurrenceIterator(rdata, zonedDateTimeToDateValue(start), TimeZone.getTimeZone(start.getZone()), strict));
    }

    /**
     * Given a block of RRULE, EXRULE, RDATE, and EXDATE content lines, parse them into a single local date iterator.
     *
     * @param rdata RRULE, EXRULE, RDATE, and EXDATE lines.
     * @param start the first occurrence of the series.
     * @param tzid the local timezone -- used to interpret any dates in RDATE and EXDATE lines that don't have TZID params.
     * @param strict true if any failure to parse should result in a ParseException. False causes bad content lines to be logged and ignored.
     */
    public static ZonedDateTimeIterator createZonedDateTimeIterator(String rdata, ZonedDateTime start, ZoneId tzid, boolean strict) throws ParseException {
        return new RecurrenceIteratorWrapper(RecurrenceIteratorFactory
            .createRecurrenceIterator(rdata, zonedDateTimeToDateValue(start.withZoneSameInstant(tzid)), TimeZone.getTimeZone(tzid), strict));
    }


    public static ZonedDateTimeIterable createZonedDateTimeIterable(String rdata, ZonedDateTime start, boolean strict) throws ParseException {
        return createZonedDateTimeIterable(rdata, start, start.getZone(), strict);
    }

    public static ZonedDateTimeIterable createZonedDateTimeIterable(String rdata, ZonedDateTime start, ZoneId tzid, boolean strict) throws ParseException {
        return new RecurrenceIterableWrapper(RecurrenceIteratorFactory
            .createRecurrenceIterable(rdata, zonedDateTimeToDateValue(start.withZoneSameInstant(tzid)), TimeZone.getTimeZone(tzid), strict));
    }

    /**
     * Creates a local date iterator given a recurrence iterator from {@link com.google.ical.iter.RecurrenceIteratorFactory}.
     */
    public static ZonedDateTimeIterator createZonedDateTimeIterator(RecurrenceIterator rit) {
        return new RecurrenceIteratorWrapper(rit);
    }


    private static final class RecurrenceIterableWrapper implements ZonedDateTimeIterable {
        private final RecurrenceIterable it;

        private RecurrenceIterableWrapper(RecurrenceIterable it) {
            this.it = it;
        }

        @Override
        public ZonedDateTimeIterator iterator() {
            return new RecurrenceIteratorWrapper(it.iterator());
        }
    }

    private static final class RecurrenceIteratorWrapper implements ZonedDateTimeIterator {
        private final RecurrenceIterator it;

        private RecurrenceIteratorWrapper(RecurrenceIterator it) {
            this.it = it;
        }

        @Override
        public void advanceTo(ZonedDateTime newStart) {
            // we need to treat midnight as a date value so that passing in
            // dateValueToDate(<some-date-value>) will not advance past any
            // occurrences of some-date-value in the iterator.
            it.advanceTo(zonedDateTimeToDateValue(newStart));
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public ZonedDateTime next() {
            return dateValueToZonedDateTime(it.next());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    static ZonedDateTime dateValueToZonedDateTime(DateValue dvUtc) {
        if (dvUtc instanceof TimeValue) {
            TimeValue tvUtc = (TimeValue) dvUtc;

            return ZonedDateTime.of(dvUtc.year(), dvUtc.month(), dvUtc.day(),
                tvUtc.hour(), tvUtc.minute(), tvUtc.second(), 0, ZoneOffset.UTC);
        } else {

            return ZonedDateTime.of(dvUtc.year(), dvUtc.month(), dvUtc.day(),
                0, 0, 0, 0, ZoneOffset.UTC);
        }
    }

    static DateValue zonedDateTimeToDateValue(ZonedDateTime date) {
        return new DateTimeValueImpl(date.getYear(), date.getMonthValue(), date.getDayOfMonth(),
            date.getHour(), date.getMinute(), date.getSecond());
    }
}
