package com.udacity.catpoint.service;

import com.udacity.catpoint.data.*;
import com.udacity.catpoint.image.FakeImageService;
import com.udacity.catpoint.image.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    private SecurityService securityService;
    @Mock
    private FakeImageService imageService;
    @Mock
    private SecurityRepository securityRepository;
    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, imageService);
    }

    @ParameterizedTest
    @CsvSource({
            "ARMED_HOME, NO_ALARM, PENDING_ALARM",
            "ARMED_AWAY, PENDING_ALARM, ALARM",
    })
    public void armedAndSensorActivated_setAlarmStatus(ArmingStatus armed, AlarmStatus initialAlarmStatus, AlarmStatus expectedAlarmStatus) {
        // Create a door sensor
        Sensor doorSensor = new Sensor("door", SensorType.DOOR);

        when(securityRepository.getArmingStatus()).thenReturn(armed);
        when(securityRepository.getAlarmStatus()).thenReturn(initialAlarmStatus);

        securityService.changeSensorActivationStatus(doorSensor,true);
        // Door sensor should become active after activation method called
        assertEquals(true, doorSensor.getActive());
        verify(securityRepository).setAlarmStatus(expectedAlarmStatus);
        verify(securityRepository).updateSensor(doorSensor);
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING_ALARM, NO_ALARM",
            "ALARM, ALARM",
    })
    public void alarm_changeInSensor_setAlarmStatus(AlarmStatus initialAlarmStatus, AlarmStatus expectedAlarmStatus) {
        // Create a door sensor
        Sensor doorSensor = new Sensor("door", SensorType.DOOR);
        doorSensor.setActive(true);
        // Create a window sensor
        Sensor windowSensor = new Sensor("window", SensorType.WINDOW);
        // Create a window sensor
        Sensor motionSensor = new Sensor("motion", SensorType.MOTION);
        // Create a Set and add the sensors to it
        Set<Sensor> sensorSet = new HashSet<>();
        sensorSet.add(windowSensor);
        sensorSet.add(motionSensor);
        when(securityRepository.getSensors()).thenReturn(sensorSet);
        when(securityRepository.getAlarmStatus()).thenReturn(initialAlarmStatus);
        securityService.changeSensorActivationStatus(doorSensor, false);
        if(expectedAlarmStatus == AlarmStatus.NO_ALARM) {
            verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
        } else {
            verify(securityRepository, never()).setAlarmStatus(any());
        }
    }

    @Test
    public void checkAllSensorsInactive_allInactiveToFalse() {
        Sensor doorSensor = new Sensor("door", SensorType.DOOR);
        doorSensor.setActive(true);
        Sensor windowSensor = new Sensor("window", SensorType.WINDOW);
        windowSensor.setActive(true);
        Set<Sensor> sensorSet = new HashSet<>();
        sensorSet.add(windowSensor);
        sensorSet.add(doorSensor);
        when(securityRepository.getSensors()).thenReturn(sensorSet);
        securityService.changeSensorActivationStatus(doorSensor, false);
        assertFalse(securityService.checkAllSensorsInactive());
    }

    @Test
    public void sensorActivatedWhileAlreadyActive_pendingAlarm_setToAlarm() {
        Sensor doorSensor = new Sensor("door", SensorType.DOOR);
        doorSensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(doorSensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @ParameterizedTest
    @EnumSource(AlarmStatus.class)
    public void sensorDeactivatedWhileAlreadyInactive_alarmStatusStayTheSame(AlarmStatus alarm) {
        Sensor doorSensor = new Sensor("door", SensorType.DOOR);
        when(securityRepository.getAlarmStatus()).thenReturn(alarm);
        securityService.changeSensorActivationStatus(doorSensor, false);
        verify(securityRepository, never()).setAlarmStatus(any());
        assertEquals(alarm, securityService.getAlarmStatus());
    }
    @Test
    public void armedHome_catDetected_setToAlarm() {
        BufferedImage fakeImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(fakeImage,50.0f)).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(fakeImage);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    public void catNotDetected_allSensorsInactive_setToNoAlarm() {
        BufferedImage fakeImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(fakeImage, 50.0f)).thenReturn(false);
        when(securityRepository.getSensors()).thenReturn(new HashSet<>());
        securityService.processImage(fakeImage);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    public void disarmed_setToNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    public void armed_resetAllSensorsToInactive() {
        Sensor doorSensor = new Sensor("door", SensorType.DOOR);
        doorSensor.setActive(true);
        Sensor windowSensor = new Sensor("window", SensorType.WINDOW);
        windowSensor.setActive(true);
        Sensor motionSensor = new Sensor("motion", SensorType.MOTION);
        motionSensor.setActive(true);
        Set<Sensor> sensorSet = new HashSet<>();
        sensorSet.add(doorSensor);
        sensorSet.add(windowSensor);
        sensorSet.add(motionSensor);
        when(securityRepository.getSensors()).thenReturn(sensorSet);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        assertEquals(false, doorSensor.getActive());
        assertEquals(false, windowSensor.getActive());
        assertEquals(false, motionSensor.getActive());
    }

    @Test
    public void catDetected_armedHome_setToAlarm() {
        BufferedImage fakeImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(fakeImage,50.0f)).thenReturn(true);
        securityService.processImage(fakeImage);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
    @ParameterizedTest
    @EnumSource(ArmingStatus.class)
    public void setArmingStatus_getsArmingStatus_updateSecurityRepo(ArmingStatus status) {
        when(securityRepository.getArmingStatus()).thenReturn(status);
        securityService.setArmingStatus(status);

        if (status == ArmingStatus.DISARMED) {
            when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
            // Verify that the alarm status is set to NO_ALARM
            assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());
        }
        // Verify that the securityRepository is called with the input parameter
        verify(securityRepository).setArmingStatus(status);
        // Verify that the getArmingStatus method from securityRepository return the same value as the input
        assertEquals(status, securityRepository.getArmingStatus());
    }
}
