package slather.g9;

import slather.sim.Cell;
import slather.sim.Point;
import slather.sim.Move;
import slather.sim.Pherome;
import java.util.*;


public class Player implements slather.sim.Player {

	private Random gen;

	//=========parameters==================================
	private static double EARLY_STAGE = 1. / 2.;
	private static double LATE_STAGE = 4. / 5.;
	private static double COMF_RANGE = 1;

	//==========functions==================================
	public void init(double d, int t, int side_length) {
		gen = new Random();
	}

    public Move invader(byte memory) {
        int role = unpackRole(memory);
        int angle = unpackAngle(memory);
        int duration = unpackDuration(memory);

        // YOUR CODE GOES HERE

        byte newMemory = packByte(role, angle, duration + 1);
		return new Move(new Point(0,0), newMemory);
    }

    public Move explorer(byte memory) {
        int role = unpackRole(memory);
        int angle = unpackAngle(memory);
        int duration = unpackDuration(memory);

        // YOUR CODE GOES HERE

        byte newMemory = packByte(role, angle, duration + 1);
		return new Move(new Point(0,0), newMemory);
    }

    public Move defender(byte memory) {
        int role = unpackRole(memory);
        int angle = unpackAngle(memory);
        int duration = unpackDuration(memory);

        // YOUR CODE GOES HERE

        byte newMemory = packByte(role, angle, duration + 1);
		return new Move(new Point(0,0), newMemory);
    }

    public Move defenderLeader(byte memory) {
        int role = unpackRole(memory);
        int angle = unpackAngle(memory);
        int duration = unpackDuration(memory);

        // YOUR CODE GOES HERE

        byte newMemory = packByte(role, angle, duration + 1);
		return new Move(new Point(0,0), newMemory);
    }

    private int unpackRole(byte memory) {
        memory = (byte) (memory >> 6);
        return memory & 0x3;
    }

    private int unpackAngle(byte memory) {
        memory = (byte) (memory >> 3);
        return memory & 0x7;
    }

    private int unpackDuration(byte memory) {
        return memory & 0x7;
    }

    private byte packByte(int role, int angle, int duration) {
        // Meaning:
        //
        // role = {0 = explorer, 1 = invader, 2 = defender, 3 = defenderLeader}
        // angle = the angle of the vector in 45-degree increments (therefore an angle value of
        //         3 indicates an actual angle of 135 degrees)
        // duration = the number of turns that the cell has followed this direction

        // Clamp values that are too large
        if (role > 3)
            role = 3;
        if (angle > 7)
            angle = 7;
        if (duration > 7)
            duration = 7;

        byte memory = 0;
        memory = (byte) role;
        memory = (byte) (memory << 3);
        memory |= (byte) angle;
        memory = (byte) (memory << 3);
        memory |= (byte) duration;

        return memory;
    }

	public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells,
                     Set<Pherome> nearby_pheromes) {
		if (player_cell.getDiameter() >= 2) {
            // One of the daughter cells maintains the current cell's role, and the other cell
            // receives a random role and angle
            int currentRole = unpackRole(memory);
            int currentAngle = unpackAngle(memory);
            int currentDuration = unpackDuration(memory);
            byte modifiedMemory = packByte(currentRole, currentAngle, currentDuration + 1);

            //int newRole = gen.nextInt(4);
            int newRole = 1;
            int newAngle = gen.nextInt(8);
            byte newMemory = packByte(newRole, newAngle, 0);

            return new Move(true, modifiedMemory, newMemory);
        }

        int role = unpackRole(memory);
        Move newMove;
        System.out.println(role);

        // Get the next move based on the current role
        if (role == 0)
            newMove = explorer(memory);
        else if (role == 1)
            newMove = invader(memory);
        else if (role == 2)
            newMove = defender(memory);
        else
            newMove = defenderLeader(memory);

        if (!collides(player_cell, newMove.vector, nearby_cells, nearby_pheromes))
            return newMove;

		// If all tries fail, just chill in place with a random angle
		return new Move(new Point(0,0), packByte(role, gen.nextInt(8), 0));
	}

	double calCrowdSurround(Cell player_cell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		int cnt = 0;
		for (int arg = 1; arg <= 180; arg += 5) {
			Point current_vector = extractVectorFromAngle(arg);
			if (collides(player_cell, current_vector, nearby_cells, nearby_pheromes))
				++cnt;
		}
		return (double)cnt / 36.;
	}

	double calCrowdInDirection(Cell player_cell, int arg, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		if (arg > 0) {
			Point vector = extractVectorFromAngle(arg);
			if (collides(player_cell, vector, nearby_cells, nearby_pheromes)) return 0;

			Point destination = player_cell.getPosition().move(vector);
			if (calCrowdSurround(player_cell, nearby_cells, nearby_pheromes) > LATE_STAGE) return 1;
		}

		for (int delta = 1; arg - delta > 0 && arg + delta <= 180 && delta < 30; ++delta) {
			Point vector1 = extractVectorFromAngle(arg - delta);
			Point vector2 = extractVectorFromAngle(arg + delta);
			if (!collides(player_cell, vector1, nearby_cells, nearby_pheromes) &&
			    !collides(player_cell, vector2, nearby_cells, nearby_pheromes))
				continue;
			return delta;
		}
		return 90;
	}

	// check if moving player_cell by vector collides with any nearby cell or hostile pherome
	private boolean collides(Cell player_cell, Point vector, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		Iterator<Cell> cell_it = nearby_cells.iterator();
		Point destination = player_cell.getPosition().move(vector);
		while (cell_it.hasNext()) {
			Cell other = cell_it.next();
			if ( destination.distance(other.getPosition()) < 0.5*player_cell.getDiameter() + 0.5*other.getDiameter() + 0.00011) 
				return true;
		}
		Iterator<Pherome> pherome_it = nearby_pheromes.iterator();
		while (pherome_it.hasNext()) {
			Pherome other = pherome_it.next();
			if (other.player != player_cell.player && destination.distance(other.getPosition()) < 0.5*player_cell.getDiameter() + 0.0001) 
				return true;
		}
		return false;
	}

	// convert an angle (in 2-deg increments) to a vector with magnitude Cell.move_dist (max allowed movement distance)
	private Point extractVectorFromAngle(int arg) {
		double theta = Math.toRadians( 2* (double)arg );
		double dx = Cell.move_dist * Math.cos(theta);
		double dy = Cell.move_dist * Math.sin(theta);
		return new Point(dx, dy);
	}

}
