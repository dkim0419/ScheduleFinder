import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Daniel on 6/4/2017.
 * ScheduleFinder reads in a csv of schedules,
 * and outputs the largest block of free time for the next 7 days between 8am to 10pm
 * csv format [id, start date, end date]:
 * 100, 2017-04-03 13:30:00, 2017-04-03 14:30:00
 */
class Schedule implements Comparable {
    public Date start;
    public Date end;

    Schedule(String s, String e) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            start = sdf.parse(s);
            end = sdf.parse(e);
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
    }

    Schedule (Date s, Date e) {
        start = s;
        end = e;
    }

    // time between other.start and this.end
    // other should start after this finishes
    long timeBetween(Schedule other) {
        return other.start.getTime() - end.getTime();
    }

    @Override
    public int compareTo(Object o) {
        int compare = start.compareTo(((Schedule)o).start);
        if (compare == 0)  return end.compareTo(((Schedule)o).end);
        return compare;
    }
}

public class ScheduleFinder {
    private static List<Schedule> parseCalendarCsv() {
        List<Schedule> schedules = new ArrayList<>();
        String file = "./src/calendar.csv";
        String line;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            while ((line = reader.readLine()) != null) {

                // use comma as separator
                String[] entry = line.split(",");
                schedules.add(new Schedule(entry[1], entry[2]));
            }
        } catch (IOException e){
            e.printStackTrace();
        }

        return schedules;
    }

    private static Schedule findLargestBlock(Map<String, List<Schedule>> map) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Schedule largest = null;
        long largestTime = 0;
        for (Map.Entry<String, List<Schedule>> entry : map.entrySet()) {
            List<Schedule> schedules = entry.getValue();
            if (schedules.size() == 0) {
                // whole day is free from 8am to 10pm
                // "yyyy-MM-dd HH:mm:ss"
                return new Schedule(entry.getKey() + " 08:00:00", entry.getKey() + " 22:00:00");
            }

            Collections.sort(schedules);

            int i = 0;
            Date min = null;
            Date max = null;
            try {
                min = sdf.parse(entry.getKey() + " 08:00:00");
                max = sdf.parse(entry.getKey() + " 22:00:00");
            } catch (ParseException e) {
                e.printStackTrace();
            }
            Schedule cur = null;
            Schedule prev = new Schedule(min, min);

            while (i <= schedules.size()) {
                if (i == schedules.size()) cur = new Schedule(max, max);
                else cur = schedules.get(i);
                long diff = prev.timeBetween(cur);
                if (diff > largestTime) {
                    largest = new Schedule(prev.end, cur.start);
                    largestTime = diff;
                }
                prev = cur;
                i++;
            }
        }

        return largest;
    }

    public static void main(String[] args) {
        List<Schedule> schedules = parseCalendarCsv();
        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd");
        // initialize hashmap of schedules for up to 1 week from now, with the date string as the key
        Map<String, List<Schedule>> scheduleMap = new HashMap<>();
        Calendar c = Calendar.getInstance();
        for (int i = 0; i < 7; i++) {
            String dateKey = keyFormat.format(c.getTime());
            scheduleMap.put(dateKey, new ArrayList<>());
            c.add(Calendar.DATE, 1);
        }

        for (Schedule s : schedules) {
            List<Schedule> val = scheduleMap.get(keyFormat.format(s.start));
            if (val != null) {
                val.add(s);
                scheduleMap.put(keyFormat.format(s.start), val);
            }
        }

        Schedule largestBlock = findLargestBlock(scheduleMap);
    }
}
