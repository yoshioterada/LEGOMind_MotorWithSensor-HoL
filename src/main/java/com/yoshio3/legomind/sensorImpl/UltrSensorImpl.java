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
package com.yoshio3.legomind.sensorImpl;

import lejos.hardware.Button;
import lejos.hardware.lcd.LCD;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.SampleProvider;
import lejos.utility.Delay;

/**
 *
 * @author Yoshio Terada
 */
public class UltrSensorImpl implements Runnable {

    //センサー・モータ
    private final EV3UltrasonicSensor ursensor;
    private final RegulatedMotor leftMotor;
    private final RegulatedMotor rightMotor;
    //障害物までの距離
    private final static int MIN_LENGTH = 3;
    private final static int MAX_LENGTH = 250;
    private final static int CRITICAL_LENGTH = 10;
    private final static int WARN_LENGTH = 20;

    //警告レベル(LED の点灯色と同一)
    private final static int DEFAULT_LEVEL = 4;  //緑色の点滅
    private final static int CRITICAL_LEVEL = 5; //赤色の点滅
    private final static int WARNING_LEVEL = 6;  //橙色の点滅

    //スレッドの停止フラグ
    private volatile boolean shutDownFlag;

    //以前の状態
    private volatile int previousStatus;

    //コンストラクタ
    public UltrSensorImpl(EV3UltrasonicSensor ursensor, RegulatedMotor leftMotor, RegulatedMotor rightMotor) {
        this.leftMotor = leftMotor;
        this.rightMotor = rightMotor;
        this.ursensor = ursensor;
        this.shutDownFlag = false;
        previousStatus = 0;
    }

    //スレッド処理を停止
    public void stop() {
        shutDownFlag = true;
    }

    //タスク処理の実装
    @Override
    public void run() {
        SampleProvider distanceMode = ursensor.getDistanceMode();
        float value[] = new float[distanceMode.sampleSize()];

        while (!shutDownFlag) {
            distanceMode.fetchSample(value, 0);
            int centimeter = (int) (value[0] * 100);
            //1mが1.000 (MIN:3cm MAX:250cm)

            if (centimeter <= CRITICAL_LENGTH) {
                executeIndividualOperation(CRITICAL_LEVEL, centimeter); //危険（回避）
            } else if (centimeter > CRITICAL_LENGTH && centimeter <= WARN_LENGTH) {
                executeIndividualOperation(WARNING_LEVEL, centimeter); //ワーニング
            } else if (Integer.MAX_VALUE != centimeter && centimeter <= MAX_LENGTH) {
                executeIndividualOperation(DEFAULT_LEVEL, centimeter); //正常
            }
            Delay.msDelay(100);
        }
    }

    //各しきい値に対する処理
    private void executeIndividualOperation(int pattern, int centimeter) {

        LCD.clearDisplay();
        LCD.drawString("Distance : " + centimeter, 0, 0);

        //前回の処理を行った時と同じステータスの場合は距離を表示して終了
        if (previousStatus == pattern) {
            return;
        }
        //前回の処理と違うステータスの場合下記を実行
        Button.LEDPattern(pattern);
        switch (pattern) {
            case DEFAULT_LEVEL: // 正常
                leftMotor.setSpeed(400);
                rightMotor.setSpeed(400);
                break;
            case CRITICAL_LEVEL: // 危険で回転
                leftMotor.stop();
                rightMotor.stop();

                leftMotor.rotate(360 + 79, false); //90度左回転
                leftMotor.waitComplete();

                leftMotor.forward();
                rightMotor.forward();
                break;
            case WARNING_LEVEL: //警告、スピードを遅く
                leftMotor.setSpeed(100);
                rightMotor.setSpeed(100);
                break;
            default:
                break;
        }
        previousStatus = pattern;
    }
}
