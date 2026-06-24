package com.demo.gym.util;

import com.demo.gym.acceptance.records.GymRecord;
import com.demo.gym.acceptance.records.MemberRecord;

import java.util.LinkedHashMap;
import java.util.Map;

public class InMemoryRecordLoader {

    public Map<String, GymRecord> loadGyms() {
        Map<String, GymRecord> hospitals = new LinkedHashMap<>();
        putUnique(hospitals, new GymRecord("St Paul Gym", "DE-001", "info@stpaul.de", "+49-1111", "https://stpaul.de", 100, "Germany", "Berlin", "Street 1", "10115"), "gym");
        putUnique(hospitals, new GymRecord("St Mary Gym", "DE-002", "info@stmary.de", "+49-2222", "https://stmary.de", 200, "Germany", "Munich", "Street 2", "80331"), "gym");
        return hospitals;
    }

    public Map<String, MemberRecord> loadMembers() {
        Map<String, MemberRecord> patients = new LinkedHashMap<>();
        putUnique(patients, new MemberRecord("John", "DE-P001", "+49-101", 1990, 1, 15, "M", "Germany", "Berlin", "St 1", "10115"), "member");
        putUnique(patients, new MemberRecord("Bob", "DE-P002", "+49-102", 1985, 3, 20, "M", "Germany", "Munich", "St 2", "80331"), "member");
        putUnique(patients, new MemberRecord("Mary", "DE-P003", "+49-103", 1992, 5, 10, "F", "Germany", "Hamburg", "St 3", "20095"), "member");
        putUnique(patients, new MemberRecord("Alice", "DE-P004", "+49-104", 1988, 7, 25, "F", "Germany", "Frankfurt", "St 4", "60311"), "member");
        putUnique(patients, new MemberRecord("Tom", "DE-P005", "+49-105", 1995, 9, 5, "M", "Germany", "Cologne", "St 5", "50667"), "member");
        putUnique(patients, new MemberRecord("Kate", "DE-P006", "+49-106", 1991, 11, 12, "F", "Germany", "Berlin", "St 6", "10115"), "member");
        return patients;
    }

    private <T> void putUnique(Map<String, T> map, T value, String recordType) {
        String key;
        if (value instanceof GymRecord gymRecord) {
            key = gymRecord.name();
        } else if (value instanceof MemberRecord memberRecord) {
            key = memberRecord.name();
        } else {
            throw new IllegalArgumentException("Unsupported record type");
        }

        if (map.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate " + recordType + " name key in fixture: " + key);
        }

        map.put(key, value);
    }
}