/*
 * Copyright 2013 Yoshio Terada
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yoshio3.legomind;

import com.yoshio3.legomind.sensorImpl.UltrSensorImpl;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lejos.hardware.BrickFinder;
import lejos.hardware.Button;
import lejos.hardware.Keys;
import lejos.hardware.ev3.EV3;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.Motor;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.SampleProvider;
import lejos.robotics.localization.OdometryPoseProvider;
import lejos.robotics.navigation.DifferentialPilot;
import lejos.utility.Delay;

/**
 *
 * @author Yoshio Terada
 */
public class EV3AppMain {

    //超音波センサー
    private static final EV3UltrasonicSensor ursensor = new EV3UltrasonicSensor(SensorPort.S4);
    //車輪モータ
    private static final RegulatedMotor leftMotor = Motor.B;
    private static final RegulatedMotor rightMotor = Motor.C;
    private ExecutorService exec;
    private UltrSensorImpl ulsensor;

    public EV3AppMain() {
        leftMotor.resetTachoCount();
        rightMotor.resetTachoCount();
        leftMotor.setSpeed(400);
        rightMotor.setSpeed(400);
    }

    public static void main(String... argv) {
        EV3AppMain main = new EV3AppMain();
        main.printHelloWorld();
        main.manageMotor();
        main.manageMotor1();
        main.manageMotor2();
        main.manageMotor3();
        main.managedByPilot();
        main.testSensor();
        main.executeConcurrentTask();
        main.onKeyTouchExit();
    }

    private void printHelloWorld() {
        LCD.drawString("Hello World", 0, 0);
        //引数：（表示文字列, x軸, y軸の表示座標）
        Button.LEDPattern(1); //0-6 までが有効
    }

    private void manageMotor() {
        leftMotor.forward();      //前進
        Delay.msDelay(3000);      //3秒間実施
        leftMotor.stop();          //停止
        leftMotor.setSpeed(100); //スピード変更
        leftMotor.backward();    //後進
        Delay.msDelay(3000);      //3秒間実施
        leftMotor.stop();          //停止
    }

    private void manageMotor1() {
        //leftMotor を使用しrotate() の動作確認
        leftMotor.rotate(360);  //①の確認
        Delay.msDelay(1000);
        leftMotor.rotate(360);  //②の確認
        Delay.msDelay(1000);
        leftMotor.rotate(0); //③の確認

    }

    private void manageMotor2() {
        //rightMotor を使用して rotateTo() の動作確認
        rightMotor.rotateTo(360);  //①の確認
        Delay.msDelay(1000);
        rightMotor.rotateTo(360);  //②の確認
        Delay.msDelay(1000);
        rightMotor.rotateTo(0);    //③の確認
    }

    private void manageMotor3() {
        //2つの引数を持つメソッドの動作確認
        leftMotor.rotate(360);
        rightMotor.rotateTo(360);
        Delay.msDelay(1000);
        leftMotor.rotate(360, true);
        rightMotor.rotateTo(360, true);
        Delay.msDelay(1000);
        leftMotor.rotate(360, false);
        rightMotor.rotateTo(360, false);
    }

    private void managedByPilot() {
        DifferentialPilot pilot = new DifferentialPilot(5.6f, 11.2f, Motor.B, Motor.C);
        OdometryPoseProvider poseProv = new OdometryPoseProvider(pilot);
        pilot.addMoveListener(poseProv);
        pilot.setTravelSpeed(50);

        //150cm 前進
        pilot.travel(150);
        pilot.stop();
    }

    private void testSensor() {
        SampleProvider distanceMode = ursensor.getDistanceMode();
        float value[] = new float[distanceMode.sampleSize()];
        //超音波センサーの場合distanceMode.sampleSize()は必ず1
        while (true) {
            distanceMode.fetchSample(value, 0);
            int centimeter = (int) (value[0] * 100);
            //1mが1.000 (MIN:3cm MAX:250cm)

            if (centimeter > 3 && centimeter <= 10) {
                executeIndividualOperation(2, centimeter);  //赤色点灯
            } else if (centimeter > 10 && centimeter <= 20) {
                executeIndividualOperation(3, centimeter); //橙色点灯
            } else if (Integer.MAX_VALUE != centimeter
                    && centimeter <= 250) {
                executeIndividualOperation(1, centimeter); //緑色点灯
            }
            Delay.msDelay(100);
        }
    }

    private void executeIndividualOperation(int pattern,
            int centimeter) {
        Button.LEDPattern(pattern);
        LCD.clearDisplay();
        LCD.drawString("Distance : " + centimeter, 0, 0);
    }

    private void executeConcurrentTask() {
        exec = Executors.newSingleThreadExecutor();
        ulsensor = new UltrSensorImpl(ursensor, leftMotor, rightMotor);
        exec.submit(ulsensor);
        leftMotor.forward();
        rightMotor.forward();
    }

    private void onKeyTouchExit() {
        EV3 ev3 = (EV3) BrickFinder.getLocal();
        Keys keys = ev3.getKeys();
        keys.waitForAnyPress();

        leftMotor.stop();
        rightMotor.stop();
        ursensor.disable();
        ulsensor.stop();
        exec.shutdownNow();
        System.exit(0);
    }
}
