/*
* This module needs to:
* 1. listen to stream of events
* 2. make a list of x events and every time before a new event
*    is added to the list,
*    the event on index 0 is removed
* 3. check if the most recent events in the list are ambiguous
* 3.1 if ambiguous, trigger orchestrator
* 3.2 if not ambiguous, trigger publisher
*/

package ambiguitypackage;

import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.List;

public class AmbiguityDetection {

    private static final long AMBIGUITY_THRESHOLD = 400; // 0.4 second threshold for ambiguity

    // File path: /Users/sejmasijaric/Documents/BA - Theory/example_events.xes
    // Reference: https://medium.com/@AlexanderObregon/javas-instant-isafter-method-explained-8b38b21f5d73#:~:text=The%20isAfter()%20method%20provides%20a%20straightforward%20and%20intuitive%20way,and%20printing%20relevant%20log%20entries.
    public boolean isAmbiguous (List<XEvent> events) {
        // Assuming two events are ambiguous if they have the same timestamp
        if (events == null || events.size() < 2) {
            System.out.println("Not enough timestamps to determine ambiguity.");
            return false;
        }
        // loop through the list of timestamps
        for (int i = 0; i < events.size() - 1; i++) {
            // Compare the current timestamp with the next one
            Instant t1 = XTimeExtension.instance().extractTimestamp(events.get(i)).toInstant();
            Instant t2 = XTimeExtension.instance().extractTimestamp(events.get(i + 1)).toInstant();
            // Check timestamp difference in milliseconds
            long differenceInMilliseconds = ChronoUnit.MILLIS.between(t1, t2);

            if (differenceInMilliseconds < AMBIGUITY_THRESHOLD) {
                System.out.println("Ambiguous events detected between: " + t1 + " and " + t2);
                return true; // Ambiguous events found
            }
        }
        return false;
    }
}
