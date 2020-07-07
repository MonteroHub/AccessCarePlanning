package com.access.careplanning;

import com.access.careplanning.viewmodel.CarePlanningViewModel;

import org.junit.Test;

import static org.junit.Assert.*;

public class CarePlanningUnitTest {

    @Test
    public void checkQR() {
        assertTrue(CarePlanningViewModel.isValidQR("WIFI_ON"));
        assertFalse(CarePlanningViewModel.isValidQR(null));
        assertFalse(CarePlanningViewModel.isValidQR(""));
        assertFalse(CarePlanningViewModel.isValidQR("wifi"));

    }

    @Test
    public void times() {
        assertEquals(0, CarePlanningViewModel.timeToMins(0, 0));
        assertEquals(60, CarePlanningViewModel.timeToMins(1, 0));
        assertEquals(65, CarePlanningViewModel.timeToMins(1, 5));

        assertEquals("1:30", MainActivity.formatTime(1,30));
        assertEquals("8:05", MainActivity.formatTime(8,5));


    }


}