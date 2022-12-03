package com.luxai.lux.utils;

import com.luxai.lux.Environment;
import com.luxai.lux.Factory;
import com.luxai.lux.Obs;
import com.luxai.lux.Robot;
import com.luxai.lux.action.UnitAction;

import java.util.Map;

public class RobotProcessor {

    public static UnitAction getActions(Obs obs, Environment environment, String player) {
        UnitAction unitAction = new UnitAction();
        Map<String, Robot> units = obs.units.get(player);
        for (Robot robot : units.values()) {
            int xRobot = robot.pos[MoveUtils.X];
            int yRobot = robot.pos[MoveUtils.Y];
            int factoryDistance = 100000;
            Factory nearestFactory = null;
            // Find nearest factory
            for (Factory factory : obs.factories.get(player).values()) {
                int manhattan = MoveUtils.getManhattanDistance(xRobot, yRobot, factory.pos[MoveUtils.X], factory.pos[MoveUtils.Y]);
                if (manhattan < factoryDistance) {
                    factoryDistance = manhattan;
                    nearestFactory = factory;
                }
            }
            if (nearestFactory != null) {
                int factoryDirection = MoveUtils.getDirection(xRobot, yRobot, nearestFactory.pos[MoveUtils.X], nearestFactory.pos[MoveUtils.Y]);
                // Cargo full
                if (robot.cargo.ice > 40) {
                    // Factory orthogonally adjacent
                    if (factoryDistance <= 3) {
                        if (robot.power > robot.getActionQueueCost(obs, environment))
                            unitAction.actions.put(robot.unit_id, robot.transfer(factoryDirection, 0, robot.cargo.ice, false));
                    }
                    // Factory long away
                    else {
                        int moveCost = robot.getMoveCost(obs, environment, player, factoryDirection);
                        if (moveCost != MoveUtils.MOVE_UNAVAILABLE
                                && robot.power >= (moveCost + robot.getActionQueueCost(obs, environment)))
                            unitAction.actions.put(robot.unit_id, robot.move(factoryDirection, false));
                    }
                }
                // Need to mine recourses
                else {
                    // Find closest ice tile
                    int iceDistance = 100000;
                    int xIce = -1;
                    int yIce = -1;
                    for (int x = 0; x < environment.map_size; x++) {
                        for (int y = 0; y < environment.map_size; y++) {
                            // Tile has ice
                            if (obs.board.ice[x][y] > 0) {
                                boolean isMyFactoryArea = false;
                                Map<String, Factory> myFactories = obs.factories.get(player);
                                for (String unitId : myFactories.keySet()) {
                                    Factory factory = myFactories.get(unitId);
                                    if (factory.isFactoryArea(x, y))
                                        isMyFactoryArea = true;
                                }
                                if (!isMyFactoryArea) {
                                    int manhattan = MoveUtils.getManhattanDistance(xRobot, yRobot, x, y);
                                    if (manhattan < iceDistance) {
                                        iceDistance = manhattan;
                                        xIce = x;
                                        yIce = y;
                                    }
                                }
                            }
                        }
                    }
                    // Robot on ice position
                    if (xIce != -1 && yIce != -1) {
                        if (xIce == xRobot && yIce == yRobot) {
                            if (robot.power >= (robot.getDigCost(obs, environment) + robot.getActionQueueCost(obs, environment)))
                                unitAction.actions.put(robot.unit_id, robot.dig(false));
                        }
                        // Ice long away
                        else {
                            int iceDirection = MoveUtils.getDirection(xRobot, yRobot, xIce, yIce);
                            int moveCost = robot.getMoveCost(obs, environment, player, iceDirection);
                            if (moveCost != MoveUtils.MOVE_UNAVAILABLE
                                    && robot.power >= (moveCost + robot.getActionQueueCost(obs, environment)))
                                unitAction.actions.put(robot.unit_id, robot.move(iceDirection, false));
                        }
                    }
                }
            }
        }
        return unitAction;
    }

}
