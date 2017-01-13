package mainplayer;
import battlecode.common.*;

public strictfp class RobotPlayer {
	
	////////////////////////////////////////
	// SIGNAL ARRAY (add new signals here)
	///////////////////////////////////////
	// 0 - Unused
	// 1 - unused
	// 2 - Enemy Sighting x
	// 3 - Enemy Sighting y
	// 4 - Archon count
	// 5 - Gardener count
	// 6 - Soldier count
	// 7 - Lumberjack count
	// 8 - Tank Count
	// 9 - Scout Count
	// 10 - unused
	// 11+ unused
	////////////////////////////////////////
	
	// Gardener, soldier, lumberjack, tank, scout
	static float[] buildOrder = {4, 3, 1, 0, 3};
	
    static RobotController rc;
    
    //direction to move in
    static Direction targetDirection = null;
    //set to 1 to go into combat, < 0 to avoid at that range, 0 to scout
    static int combat = -100;
    //has this unity seen the enemy yet
    static boolean seenEnemy = false;
    //
    static int[] signalLoc = new int[2];
    //
    static boolean broadcastDeath = false;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
    	

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

       
        
        // Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
        switch (rc.getType()) {
            case ARCHON:
                runArchon();
                break;
            case GARDENER:
                runGardener();
                break;
            case SOLDIER:
                runSoldier();
                break;
            case LUMBERJACK:
                runLumberjack();
                break;
            case TANK:
                runSoldier();
                break;
            case SCOUT:
            	runScout();
                break;
        }
	}

    //Archon code. Has the highest bytecode so it is best to do command type stuff here
    static void runArchon() throws GameActionException {
        System.out.println("I'm an archon!");
        
        Team enemy = rc.getTeam().opponent();
        //flee to range of 10
        combat = -1000;
        //the bullets the team had the last time the loop ran
        float lastBullets = 300;
        //bullets gained since last donation
        float income = 0;

        //broadcast unit creation
        rc.broadcast(4,rc.readBroadcast(4) + 1);
        
        
        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
            	
            	if(!broadcastDeath && rc.getHealth() < 20) {
            		rc.broadcast(4,rc.readBroadcast(4) - 1);
            		broadcastDeath = true;
            	}

                // Generate a random direction
                Direction dir = randomDirection();


                // Randomly attempt to build a gardener in this direction
                // TODO come up with a better way to decide when to do this
                if ( rc.canHireGardener(dir) && chooseProduction(true) == 0) {
                    rc.hireGardener(dir);
                }
                
                
                //spot enemies and call for help
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
                if (robots.length > 0) {
                	rc.broadcast(2,(int)robots[0].location.x);
                    rc.broadcast(3,(int)robots[0].location.y);
                }
                else {
                	//TODO clear the broadcasts if old
                	
                	//TODO send units to an enemy location (spy on enemy brodcasts)
                }

                // Move randomly
                //TODO improve on this
                wander(10);

//                // Broadcast archon's location for other robots on the team to know
//                //TODO use this in some way or get rid of it
//                MapLocation myLocation = rc.getLocation();
//                rc.broadcast(0,(int)myLocation.x);
//                rc.broadcast(1,(int)myLocation.y);
                
                //TODO buy victory points
                float bullets = rc.getTeamBullets();
                
                if(bullets > lastBullets)
                {
                	income += bullets - lastBullets;
	                if(income >= 100 * rc.readBroadcast(4) && bullets >= 100) {
	                	System.out.println("Thank You! ");
	                	income -= 100 * rc.readBroadcast(4);
	                	rc.donate(10 * (1 + (rc.getRoundNum()/300)));
	                	
	                }
                }

                lastBullets = rc.getTeamBullets();
                
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }

	static void runGardener() throws GameActionException {
        System.out.println("I'm a gardener!");
        Team enemy = rc.getTeam().opponent();
        Team myTeam = rc.getTeam();
        combat = -60;
        
        //broadcast unit creation
        rc.broadcast(5,rc.readBroadcast(5) + 1);
       
        Direction buildAxis = null;
        
        boolean water = false;

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
            	if(!broadcastDeath && rc.getHealth() < 20) {
            		rc.broadcast(5,rc.readBroadcast(5) - 1);
            		broadcastDeath = true;
            	}


                // Generate a random direction
            	Direction dir;
            	
            	if(buildAxis == null)
            		dir = randomDirection();
            	else
            		dir = buildAxis;
                
                //call for help
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
                if (robots.length > 0) {
                	rc.broadcast(2,(int)robots[0].location.x);
                    rc.broadcast(3,(int)robots[0].location.y);
                }
                
                
              

                // Attempt to build a soldier or lumberjack in this direction
                //TODO come up with a better way for doing this
                int buildType = chooseProduction(false);
                
                if (buildType == 1 && rc.canBuildRobot(RobotType.SOLDIER, dir)) {
                    rc.buildRobot(RobotType.SOLDIER, dir);
                } else if (buildType == 2 && rc.canBuildRobot(RobotType.LUMBERJACK, dir)) {
                    rc.buildRobot(RobotType.LUMBERJACK, dir);
                }
                else if (buildType == 4 && rc.canBuildRobot(RobotType.SCOUT, dir)) {
                    rc.buildRobot(RobotType.SCOUT, dir);
                }
                
               
                //plant a tree
                if(buildAxis != null || (!rc.isCircleOccupied(rc.getLocation().add(dir, 2.01f), 1) 
                		&& rc.onTheMap(rc.getLocation().add(dir, 2.01f), 1) && rc.senseNearbyRobots(4).length == 0)) {
                	
                	for(int addDir = 60; addDir < 360; addDir += 60) {
	                	if(rc.canPlantTree(dir.rotateRightDegrees(addDir))) {
	                		rc.plantTree(dir.rotateRightDegrees(addDir));
	                		buildAxis = dir;
	                		water = true;
	                	}
                	}
                }
            	
              
               
               //Get the info on the targeted tree
               TreeInfo treeInfo = null;
               boolean canWaterTreeInfo = false;
         	   //if it can't be sensed then pick a new tree
         	   TreeInfo[] trees = rc.senseNearbyTrees();
     		   for(TreeInfo tree : trees) {
         		   //Pick a new tree to target
         		   //TODO use a better algorithm for doing this. Maybe check for bullets or robots or pick closest one
         		   if(tree.getTeam() == myTeam) {
         			   boolean canWaterTree = rc.canWater(tree.ID);
         			   if(water == true && (treeInfo == null || tree.health < treeInfo.health || (canWaterTree && !canWaterTreeInfo))) {
         				   treeInfo = tree;
         				   canWaterTreeInfo = canWaterTree;
         				   
         			   }
         		   }
     		   }
         	   
                
                // if a tree is targeted move closer and water it if close enough
 	       		if(treeInfo != null) {
       			    if(canWaterTreeInfo) {
       					rc.water(treeInfo.ID);
       					
       				}
       				else {
       					tryMove(directionTwords( rc.getLocation(), treeInfo.location));
       					
       					if(!rc.hasMoved())
       	 	       			wander(10);
       				}
 	       			
                 }
 	       		else {
                // Move randomly
                //TODO improve on this
 	       		if(!rc.hasMoved())
	       			wander(10);
 	       		}
 	       		

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }
	

    static void runSoldier() throws GameActionException {
        System.out.println("I'm an soldier!");
        Team enemy = rc.getTeam().opponent();
        
        
      //broadcast unit creation
        rc.broadcast(6,rc.readBroadcast(6) + 1);

        // The code you want your robot to perform every round should be in this loop
        while (true) {
        	
        	
        	combat = 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
            	if(!broadcastDeath && rc.getHealth() < 20) {
            		rc.broadcast(6,rc.readBroadcast(6) - 1);
            		broadcastDeath = true;
            	}
            	
                MapLocation myLocation = rc.getLocation();

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

	                
                // If there are some...
                if (robots.length > 0) {
                	
                    
                    int closest = 0;
                    float smallestDistance = Float.MAX_VALUE;
                   
                    for(int i = 0; i < robots.length; i++) {
                    	float distanceTo = robots[i].getLocation().distanceTo(rc.getLocation());
                    	if(distanceTo < smallestDistance) {
                    		closest = i;
                    		smallestDistance = distanceTo;
                    	}
                    }
                    
                    rc.broadcast(2,(int)robots[closest].location.x);
                    rc.broadcast(3,(int)robots[closest].location.y);
                    
                    if (robots.length > 2 && rc.canFireTriadShot()) {
                    	 rc.fireTriadShot(myLocation.directionTo(robots[closest].location));
                    }
                    
                    // And we have enough bullets, and haven't attacked yet this turn...
                    if (rc.canFireSingleShot()) {
                        // ...Then fire a bullet in the direction of the enemy.
                        rc.fireSingleShot(myLocation.directionTo(robots[0].location));
                        
                    }
                    
                    if(robots[closest].type == RobotType.ARCHON || robots[closest].type == RobotType.GARDENER  ||
                    		Math.sqrt(smallestDistance) > Math.sqrt(rc.getType().sensorRadius * rc.getType().sensorRadius) * .7)
                    	tryMove(directionTwords( rc.getLocation(), robots[closest].location));
                    else
                    	tryMove(directionTwords( robots[closest].location, rc.getLocation()));
                }
              

                	// Move randomly
                if(!rc.hasMoved())
                	wander(10);
                

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
    }
    
    
    static void runScout() throws GameActionException {
        System.out.println("I'm an Scout!");
        Team enemy = rc.getTeam().opponent();
        
        
      //broadcast unit creation
        rc.broadcast(9,rc.readBroadcast(9) + 1);

        // The code you want your robot to perform every round should be in this loop
        while (true) {
        	
        	
        	combat = 0;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
            	if(!broadcastDeath && rc.getHealth() < 15) {
            		rc.broadcast(9,rc.readBroadcast(9) - 1);
            		broadcastDeath = true;
            	}
            	
                MapLocation myLocation = rc.getLocation();

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

	                
                // If there are some...
                if (robots.length > 0) {
                	
                    
                    int closest = 0;
                    float smallestDistance = Float.MAX_VALUE;
                   
                    for(int i = 0; i < robots.length; i++) {
                    	float distanceTo = robots[i].getLocation().distanceTo(rc.getLocation());
                    	if(distanceTo < smallestDistance) {
                    		closest = i;
                    		smallestDistance = distanceTo;
                    	}
                    }
                    
                    rc.broadcast(2,(int)robots[closest].location.x);
                    rc.broadcast(3,(int)robots[closest].location.y);
                    
                    if (robots.length > 2 && rc.canFireTriadShot()) {
                    	 rc.fireTriadShot(myLocation.directionTo(robots[closest].location));
                    }
                    
                    // And we have enough bullets, and haven't attacked yet this turn...
                    if (rc.canFireSingleShot()) {
                        // ...Then fire a bullet in the direction of the enemy.
                        rc.fireSingleShot(myLocation.directionTo(robots[closest].location));
                        
                    }
                    
                    if(robots[closest].type == RobotType.ARCHON || robots[closest].type == RobotType.GARDENER ||
                    		Math.sqrt(smallestDistance) > Math.sqrt(rc.getType().sensorRadius * rc.getType().sensorRadius) * .7)
                    	tryMove(directionTwords( rc.getLocation(), robots[closest].location));
                    else
                    	tryMove(directionTwords( robots[closest].location, rc.getLocation()));
                }
              

                	// Move randomly
                if(!rc.hasMoved())
                	wander(10);
                

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Scout Exception");
                e.printStackTrace();
            }
        }
    }

    static void runLumberjack() throws GameActionException {
        System.out.println("I'm a lumberjack!");
        Team enemy = rc.getTeam().opponent();
        combat = 0;
        int targetTree = -1;
        boolean standStill = false;
        
        //broadcast unit creation
        rc.broadcast(7,rc.readBroadcast(7) + 1);

        // The code you want your robot to perform every round should be in this loop
        while (true) {
        	
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
            	if(!broadcastDeath && rc.getHealth() < 20) {
            		rc.broadcast(7,rc.readBroadcast(7) - 1);
            		broadcastDeath = true;
            	}

                // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
                RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius+GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);

               
                
                
                if(robots.length > 0 && !rc.hasAttacked()) {
                    // Use strike() to hit all nearby robots!
                    rc.strike();
                } 
                    // No close robots, so search for robots within sight radius
                    robots = rc.senseNearbyRobots(-1,enemy);
                    
                    //find the closest robot...
                    if(robots.length > 0) {

	                    int closest = 0;
	                    float smallestDistance = Float.MAX_VALUE;
	                   
	                    for(int i = 0; i < robots.length; i++) {
	                    	float distanceTo = robots[i].getLocation().distanceTo(rc.getLocation());
	                    	if(distanceTo < smallestDistance) {
	                    		closest = i;
	                    		smallestDistance = distanceTo;
	                    	}
	                    }
	                    
	                    //broadcast its location
	                    rc.broadcast(2,(int)robots[closest].location.x);
	                    rc.broadcast(3,(int)robots[closest].location.y);
	                    
	                    
	                    //and move closer to it
	                    tryMove(directionTwords( rc.getLocation(), robots[closest].location));
                    
                }
                
               //chop down neutral or enemy trees tree
                
               //Get the info on the targeted tree
               TreeInfo treeInfo = null;
               standStill = false;
               
               if(targetTree != -1) {
            	   if(rc.canSenseTree(targetTree))
            		   treeInfo = rc.senseTree(targetTree);
            	   else
            		   targetTree = -1;
               }
               if(targetTree == -1){
            	   //if it can't be sensed then pick a new tree
            	   TreeInfo[] trees = rc.senseNearbyTrees();
            	   
            	   if(trees.length > 1) {
	            	  
	                   float smallestDistanceToTree = Float.MAX_VALUE;
	                  
	                   for(int i = 0; i < trees.length; i++) {
	                   		float distanceTo = trees[i].getLocation().distanceTo(rc.getLocation());
	                   		if(trees[i].team != rc.getTeam() && distanceTo < smallestDistanceToTree) {
	                   			treeInfo = trees[i];
	                   			targetTree = treeInfo.ID;
	                   			smallestDistanceToTree = distanceTo;
	                   		}
	                   }
            	   }
                   
               }
               // if a tree is targeted move closer to it or chop it
	       		if(treeInfo != null) {
	       			
	       			System.out.println("try chop " + treeInfo.ID);
	       			if(rc.canChop(treeInfo.ID) && !rc.hasAttacked()) {
	        			rc.chop(treeInfo.ID);
	        			standStill = true;
	        		}	
	       			else if (!rc.hasMoved()) {
	        			tryMove(directionTwords( rc.getLocation(), treeInfo.location));
	        		}
       				
       				
	       			
                }

            	
            	// Move randomly
	       		// TODO improve on this
            	if(!standStill && !rc.hasMoved()) {
            		wander(10);
            	}

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Lumberjack Exception");
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns a random Direction
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }
    
    static Direction directionTwords(MapLocation objLoc, MapLocation towardsLoc) {
		return new Direction(objLoc, towardsLoc);
	}
    
    
    static void wander(int tries) throws GameActionException {
    	if(tries <= 0)
    		return;
    
    	if(combat != 0 && targetDirection != null) {
    		//run to
    		int broadcastOne = rc.readBroadcast(2);
    		int broadcastTwo = rc.readBroadcast(3);
    		if( (broadcastOne != 0 || broadcastTwo != 0) && (signalLoc[0] != broadcastOne || signalLoc[1] != broadcastTwo)) {
    			MapLocation newLoc = new MapLocation(broadcastOne, broadcastTwo);
    			if(combat > 0) {
    				
    				targetDirection = new Direction(rc.getLocation(), newLoc);
    				signalLoc[0] = broadcastOne;
    				signalLoc[1] = broadcastTwo;
    			}
    			else if(combat < 0) { 
    				//run away if close
    				MapLocation myLoc = rc.getLocation();
    				if(myLoc.distanceSquaredTo(newLoc) < -combat) {
	    				targetDirection = new Direction(newLoc, myLoc);
	    				signalLoc[0] = broadcastOne;
	    				signalLoc[1] = broadcastTwo;
    				}
    			}
    		}
    	}
    	
    	
    	if(targetDirection == null || Math.random() < .05f)
    		targetDirection = randomDirection();
    	
    	if(!tryMove(targetDirection)) {
    		targetDirection = null;
    		wander(tries - 1);
    	}
	
    	
    }
    
    static int chooseProduction(boolean archon) throws GameActionException {

    	//check for victory
    	float bullets = rc.getTeamBullets();
    	if((int)(bullets / 10) + rc.getTeamVictoryPoints() >= 1000) {
        	rc.donate(bullets);
        }
    	
    	//Calculate the number of each unit compared to the desired number
    	float[] armyRatios = {(float)rc.readBroadcast(5) / buildOrder[0], 
    			(float)rc.readBroadcast(6) / buildOrder[1], (float)rc.readBroadcast(7) / buildOrder[2],
    			(float)rc.readBroadcast(8) / buildOrder[3], (float)rc.readBroadcast(9) / buildOrder[4]};
    	
    	//store the best one to create
    	int bestRatio = 1;
    	
    	
    	
    	//see if there are any archons or if this unit is an archon. Only in this case can gardeners be produced
    	if(archon || rc.readBroadcast(4) > 0) {
    		
    		if(rc.readBroadcast(5) < 2)
    			return 0;
    					
    		bestRatio = 0;
    		//if there are no gardeners and one could be made, make one
    		if(armyRatios[0] == 0 || rc.getTeamBullets() > 400) {
    			return 0;
    		}
    	}
    	//iterate through each unit type and select the best one to produce next
    	for(int i = 1 + bestRatio; i < armyRatios.length; i++) {
    		System.out.println(" " + armyRatios[i] + " " + armyRatios[bestRatio]);
    		if(armyRatios[i] < armyRatios[bestRatio]) {
    			bestRatio = i;
    		}
    	}
    	
    	if(bestRatio != 0 && rc.senseNearbyTrees(-1, Team.NEUTRAL).length > 0)
    		return 2;
    	
    	System.out.println("Build " + bestRatio);
    	return bestRatio;
    	
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,20,3);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of movement
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }

    /**
     * A slightly more complicated example function, this returns true if the given bullet is on a collision
     * course with the current robot. Doesn't take into account objects between the bullet and this robot.
     *
     * @param bullet The bullet in question
     * @return True if the line of the bullet's path intersects with this robot's current position.
     */
    static boolean willCollideWithMe(BulletInfo bullet) {

        MapLocation myLocation = rc.getLocation();
        
        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI/2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
    }
}