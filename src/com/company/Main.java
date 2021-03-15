package com.company;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Logger;


public class Main {

    private static volatile boolean state = true;
    private static final String UNSUBSCRIBE = "@a0",
            SUBSCRIBE = "@a1", TRANSMIT = "transmit 1", NO_TRANSMIT = "transmit 0";
    private static final int MAX_VOLTAGE_ON_ANALOG_INPUT = 143;
    private static final int MAX_TEMP_ON_TEMP_INPUT = 1000;
    private static final int MAX_CELL_VOLTAGE = 4250;
    private static final int REDUCTION = 8;
    private static int batteryCapacity = 0,
            batteryVoltage = 0,
            batteryCurrent = 0,
            speed = 0,
            range = 0,
            passedDistance = 0,
            totalDistance = 1200,
            rpm = 0;
    private static volatile boolean flag = true,
            flagSendAllParams = true,
            flagCapacity = true,
            flagSpeed = true,
            flagVoltage = true,
            flagCurrent = true,
            flagTransmit = false;
    static int cellNum = 1;


    public static void main(String[] args) {

        System.out.println("SERVER START");

//        Logger logger = Logger.getLogger(Main.class.getName());
//        logger.log(Level.INFO, "");
//        logger.warning("");

        try (ServerSocket ss = new ServerSocket(8080)) {

            while (state) {
                Socket s = ss.accept();
                System.out.println(Thread.currentThread().getName() + ": CLIENT " + s.getInetAddress() + " CONNECTED");
                flag = true;
                new Thread(() -> {

                    try {
                        Scanner scanner = new Scanner(s.getInputStream());
                        PrintWriter writer = new PrintWriter(s.getOutputStream(), true);
                        //input stream thread
                        new Thread(() -> {
                            while (flag) {
                                try {
                                    System.out.println(Thread.currentThread().getName() + " -> input: WAIT COMMAND FROM " + s.getInetAddress());
                                    String request = scanner.nextLine();
                                    System.out.println(Thread.currentThread().getName() + " -> input: CLIENT " + s.getInetAddress() + " SAY: " + request);

                                    switch (request) {
                                        case UNSUBSCRIBE:
                                            if (flagSendAllParams) {
                                                flagSendAllParams = false;
                                                System.out.println(Thread.currentThread().getName() + " -> input: STATUS -> UNSUBSCRIBE\n");
                                            }
                                            break;
                                        case SUBSCRIBE:
                                            if (!flagSendAllParams) {
                                                flagSendAllParams = true;
                                                System.out.println(Thread.currentThread().getName() + " -> input: STATUS -> SUBSCRIBE\n");
                                            }
                                            break;
                                        case TRANSMIT:
                                            if (flagSendAllParams) {
                                                flagSendAllParams = false;
                                            }
                                            flagTransmit = true;
                                            System.out.println(Thread.currentThread().getName() + " -> input: STATUS -> TRANSMIT\n");
                                            break;
                                        case NO_TRANSMIT:
                                            flagTransmit = false;
                                            System.out.println(Thread.currentThread().getName() + " -> input: STATUS -> DONT TRANSMIT\n");

                                    }
                                } catch (Exception e) {


                                    try {
                                        System.out.println(Thread.currentThread().getName() + " -> input: Client " + s.getInetAddress() + " disconnected");
                                        flag = false;
                                        state = false;
                                        s.close();
                                        Thread.currentThread().interrupt();
                                    } catch (IOException ioException) {
                                        ioException.printStackTrace();
                                    }

                                }
                            }
                        }).start();

                        //output stream thread
                        new Thread(() -> {

                            try {

                                while (flag) {
                                    if (flagSendAllParams) {
                                        String temp = getValue();
                                        writer.println(temp);
//                                        System.out.println(temp);
                                        Thread.sleep(50);
                                    } else if (flagTransmit) {
                                        String temp = getTrasmit();
                                        writer.println(temp);
//                                        System.out.println(temp);
                                        Thread.sleep(50);
                                    }
                                }
                            } catch (Exception e) {
                                try {
                                    System.out.println(Thread.currentThread().getName() + " -> output: Client " + s.getInetAddress() + " disconnected");
                                    flag = false;
                                    s.close();
                                    state = false;
                                    Thread.currentThread().interrupt();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }).start();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }).start();
                state = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("End MAIN");
    }

    private static String getTrasmit() {
        StringBuilder sb = new StringBuilder();
        sb.append("cell");
        sb.append(cellNum);
        sb.append(": ");
        sb.append(new Random().nextInt(1750) + 2500);
        sb.append(",");
        sb.append(new Random().nextInt(65) - 25);
        sb.append(",");
        sb.append(new Random().nextInt(50) - 15);
        sb.append(",");
        sb.append("5,8");

        cellNum = cellNum < 34 ? cellNum += 1 : 1;
        return sb.toString();
    }

    public static String getValue() {

        char[] chars = new char[]{'V', 'l', 'n', 'm', 'w', 'z', 'q', 'i', 't', 'v', 'c', 'f', 'R', 'F', 'r', 's', 'E'}; //, 'V', 'l', 'n', 'm', 'w', 'z', 'q', 'i', 't'
        Random random = new Random();
        StringBuilder stringBuilder = new StringBuilder();
        String result = "&E0";
        int randomCell = random.nextInt(95);
        int randomVoltageCell = random.nextInt(1200) + 3000;
        int randomTempCell = random.nextInt(600) - 100;
        char symbolOfParameter = chars[random.nextInt(chars.length - 1)];

        if (symbolOfParameter == 'm' || symbolOfParameter == 'n') {
            stringBuilder.append("&");
            stringBuilder.append(symbolOfParameter);
            stringBuilder.append(randomCell);
            stringBuilder.append(":");
            stringBuilder.append(randomVoltageCell);
            result = stringBuilder.toString();

            result = "&" + symbolOfParameter + "" + randomCell + ":" + randomVoltageCell;
        }
        if (symbolOfParameter == 'z' || symbolOfParameter == 'w') {
            result = "&" + symbolOfParameter + "" + randomCell + ":" + randomTempCell;
        }
        if (symbolOfParameter == 'i') {
            result = "&" + symbolOfParameter +
                    ":" + random.nextInt(MAX_VOLTAGE_ON_ANALOG_INPUT) +
                    ":" + random.nextInt(MAX_VOLTAGE_ON_ANALOG_INPUT) +
                    ":" + random.nextInt(MAX_VOLTAGE_ON_ANALOG_INPUT) +
                    ":" + random.nextInt(MAX_VOLTAGE_ON_ANALOG_INPUT) +
                    ":" + random.nextInt(MAX_VOLTAGE_ON_ANALOG_INPUT) +
                    ":" + random.nextInt(MAX_VOLTAGE_ON_ANALOG_INPUT);
        }
        if (symbolOfParameter == 't') {
            result = "&" + symbolOfParameter +
                    ":" + random.nextInt(MAX_TEMP_ON_TEMP_INPUT) +
                    ":" + random.nextInt(MAX_TEMP_ON_TEMP_INPUT) +
                    ":" + random.nextInt(MAX_TEMP_ON_TEMP_INPUT) +
                    ":" + random.nextInt(MAX_TEMP_ON_TEMP_INPUT);
        }
        if (symbolOfParameter == 'q') {
            result = "&" + symbolOfParameter + randomCell +
                    ":" + random.nextInt(MAX_CELL_VOLTAGE) +
                    ":" + (random.nextInt(800) - 200) +
                    ":" + (random.nextInt(800) - 200) +
                    ":" + "0" +
                    ":" + "1";

        }
        if (symbolOfParameter == 'l') {
            if (batteryCapacity < 1000 && flagCapacity) {
                batteryCapacity += random.nextInt(20) + 10;
                result = "&" + symbolOfParameter + "" + batteryCapacity;
            }
            if (batteryCapacity > 0 && !flagCapacity) {
                batteryCapacity -= random.nextInt(20) + 10;
                if (batteryCapacity >= 0) {
                    result = "&" + symbolOfParameter + "" + batteryCapacity;
                }
            }
            if (batteryCapacity <= 0) {
                flagCapacity = true;
            } else if (batteryCapacity >= 1000) {
                flagCapacity = false;
            }
        }
        if (symbolOfParameter == 'R') {
            range = map(batteryCapacity, 0, 1000, 0, 3000);
            result = "&" + symbolOfParameter + "" + range;
        }
        if (symbolOfParameter == 'f') {
            passedDistance = 3000 - range;
            totalDistance += 10;
            result = "&" + symbolOfParameter + "" + passedDistance;
        }
        if (symbolOfParameter == 'F') {
            result = "&" + symbolOfParameter + "" + totalDistance;
        }
        if (symbolOfParameter == 'r') {
            rpm = speed * REDUCTION;
            result = "&" + symbolOfParameter + "" + rpm;
        }
        if (symbolOfParameter == 'v') {
            if (batteryVoltage < 4050 && flagVoltage) {
                batteryVoltage += random.nextInt(40) + 10;
                result = "&" + symbolOfParameter + "" + batteryVoltage;
            }
            if (batteryVoltage > 2500 && !flagVoltage) {
                batteryVoltage -= random.nextInt(40) + 10;
                if (batteryVoltage >= 0) {
                    result = "&" + symbolOfParameter + "" + batteryVoltage;
                }
            }
            if (batteryVoltage <= 2500) {
                flagVoltage = true;
            } else if (batteryVoltage >= 4050) {
                flagVoltage = false;
            }
        }
        if (symbolOfParameter == 'c') {
            if (batteryCurrent < 2500 && flagCurrent) {
                batteryCurrent += random.nextInt(40) + 10;
                result = "&" + symbolOfParameter + "" + batteryCurrent;
            }
            if (batteryCurrent > 0 && !flagCurrent) {
                batteryCurrent -= random.nextInt(40) + 10;
                if (batteryCurrent >= 0) {
                    result = "&" + symbolOfParameter + "" + batteryCurrent;
                }
            }
            if (batteryCurrent <= 0) {
                flagCurrent = true;
            } else if (batteryCurrent >= 2500) {
                flagCurrent = false;
            }
        }
        if (symbolOfParameter == 'V') {
            if (speed < 180 && flagSpeed) {
                speed += random.nextInt(9) + 1;
                result = "&" + symbolOfParameter + "" + speed;
            }
            if (speed > 0 && !flagSpeed) {
                speed -= random.nextInt(9) + 1;
                if (speed >= 0) {
                    result = "&" + symbolOfParameter + "" + speed;
                }
            }
            if (speed <= 0) {
                flagSpeed = true;
            } else if (speed >= 180) {
                flagSpeed = false;
            }
        }
        if (symbolOfParameter == 's') {
            result = "&" + symbolOfParameter + "" + random.nextInt(13);
        }
        if (symbolOfParameter == 'E') {
            String[] arr = new String[]{"0", "0x1", "0x2", "0x4", "0x8", "0x10", "0x20", "0x40", "0x100", "0x200", "0x400"};
            result = "&" + symbolOfParameter + arr[random.nextInt(arr.length - 1)];
        }
        return result;
    }

    public static int map(int x, int in_min, int in_max, int out_min, int out_max) {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }
}
