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

	public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		//if (player_cell.getDiameter() >= 2 /*&& (calCrowdInDirection(player_cell, (int)memory, nearby_cells, nearby_pheromes) > COMF_RANGE)*/) // reproduce when it is not very crowd
		//	return new Move(true, (byte) 0, (byte) 1);
		
		/*
		if (memory > 0) { // follow previous direction in early stage, try least crowd direction in later
			if (calCrowdSurround(player_cell, nearby_cells, nearby_pheromes) < EARLY_STAGE) {
				Point vector = extractVectorFromAngle( (int)memory);
				// check for collisions
				if (!collides( player_cell, vector, nearby_cells, nearby_pheromes))
					return new Move(vector, memory);
			}
		}
	*/
		/*
		Move m;
		if(memory == 0){
		 m = defender(player_cell, memory, nearby_cells, nearby_pheromes);
		
		} else{
			m = leaderDefender(player_cell, memory, nearby_cells, nearby_pheromes);
		}
		if(m != null){
			return m;
		}
		*/
		int old_role = ((int)memory & (3));	
		int old_dir = ((int)memory >> 2);
		
		int decision;
		if (old_role == 0) decision = defender(player_cell, (int)memory, nearby_cells, nearby_pheromes); else
		if (old_role == 1) decision = leaderDefender(player_cell, (int)memory, nearby_cells, nearby_pheromes); else
		if (old_role == 2) decision = explore(player_cell, (int)memory, nearby_cells, nearby_pheromes); else
		if (old_role == 3) decision = invasive(player_cell, (int)memory, nearby_cells, nearby_pheromes);

		if (player_cell.getDiameter() >= 2) {
			int daughter_decision;
			int daughter_role = gen.nextInt(4);
			int daughter_memory = encode(daughter_role, old_dir);
			if (daguther_role == 0)
				daughter_decision = defender(player_cell, daughter_memory, nearby_cells, nearby_pheromes); else
			if (daughter_role == 1)
				daughter_decision = leaderDefender(player_cell, daughter_memory, nearby_cells, nearby_pheromes); else
			if (daughter_role == 2)
				daughter_decision = explore(player_cell, daughter_memory, nearby_cells, nearby_pheromes); else
			if (daughter_role == 3)
				daughter_decision = invasive(player_cell, daughter_memory, nearby_cells, nearby_pheromes);

			return new Move(true, (byte)decision, (byte)daughter_decision);
		}

		Point vector = extractVectorFromAngle(decision >> 2);
		return new Move(vector, (byte)decision);
		
		/*
		// otherwise, try random directions to go in until one doesn't collide
		double max_range = 0;
		int direction = 0;
		Point vector = new Point(0, 0);
		for (int arg = 1; arg <= 60; arg += 5) {
			Point current_vector = extractVectorFromAngle(arg);
			if (collides(player_cell, current_vector, nearby_cells, nearby_pheromes)) continue;
			double current_range = calCrowdInDirection(player_cell, arg, nearby_cells, nearby_pheromes);
			if (current_range > max_range) {
				max_range = current_range;
				direction = arg;
				vector = current_vector;
			} else
			if (current_range == max_range) {
				int tmp = gen.nextInt(10);
				if (tmp < 1) {
					direction = arg;
					vector = current_vector;
				}
			}
		}
		if (!collides(player_cell, vector, nearby_cells, nearby_pheromes))
			return new Move(vector, encode(role, direction));
	
		
		// if all tries fail, just chill in place
		return new Move(new Point(0,0), encode(role, direction));
		*/
	}

	private int leaderDefender(Cell player_cell, int memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes){

		/*
		Point vector = extractVectorFromAngle(memory);
		// check for collisions
		if (!collides( player_cell, vector, nearby_cells, nearby_pheromes)){
			return memory;
		}

		// if no previous direction specified or if there was a collision, try random directions to go in until one doesn't collide
		for (int i=0; i<20; i++) {
			int arg = gen.nextInt(60) + 1;
			vector = extractVectorFromAngle(arg);
			if (!collides(player_cell, vector, nearby_cells, nearby_pheromes)) {
				return encode((memory & 3), arg);
			}
		}
		return memory;
		*/

		int min_items = INF;
		int direction = 0;
		int jump = 5;
		for (int arg = 1; arg <= 60; arg += ((arg % 3 != 0)?(1):(jump))) {
			Point current_vector = extractVectorFromAngle(arg);
			if (collides(player_cell, current_vector, nearby_cells, nearby_pheromes)) continue;

			int cnt = calNumPherome(arg, jump / 2, player_cell, nearby_cells, nearby_pheromes);
			if (cnt < min_items) {
				min_items = cnt;
				direction = arg;
			}
		}

		return encode((memory & 3), direction);

	}

	private int defender(Cell player_cell, int memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes){
		/*
		for(Pherome p: nearby_pheromes){
			if(p.player == player_cell.player){
				Point cellPosition = player_cell.getPosition();
				Point pheromePosition = p.getPosition();
				if(cellPosition.distance(pheromePosition) <= 2 && cellPosition.distance(pheromePosition) > 1 ){
					
					Point vector = cellPosition.move(pheromePosition);
					Move m = new Move(vector, memory);
					if(!collides(player_cell, vector,nearby_cells,nearby_pheromes)){

						return m;
					}

				}
			}
		}
		
		return leaderDefender(player_cell, (byte) 1, nearby_cells, nearby_pheromes); //change the role to leaderDefender
		*/
		int old_dir = memory >> 2;
		int new_dir = ((old_dir + 29) % 180) + 1;
		return encode((memory & 3), new_dir);
	}

	private int explore(Cell player_cell, int memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		int min_items = INF;
		int direction = 0;
		int jump = 5;
		for (int arg = 1; arg <= 60; arg += ((arg % 3 != 0)?(1):(jump))) {
			Point current_vector = extractVectorFromAngle(arg);
			if (collides(player_cell, current_vector, nearby_cells, nearby_pheromes)) continue;

			int cnt = calNumItem(arg, jump / 2, player_cell, nearby_cells, nearby_pheromes);
			if (cnt < min_items) {
				min_items = cnt;
				direction = arg;
			}
		}

		return encode((memory & 3), direction);
	}

	private int Invasive(Cell player_cell, int memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		int max_items = 0;
		int direction = 0;
		int jump = 5;
		for (int arg = 1; arg <= 60; arg += (arg % 3 != 0)?(1):(jump)) {
			Point current_vector = extractVectorFromAngle(arg);
			if (collides(player_cell, current_vecotr, nearby_cells, nearby_pheromes)) continue;

			int cnt = calNumItem(arg, jump / 2, player_cell, nearby_cells, nearby_pheromes);
			if (cnt > max_items) {
				max_items = cnt;
				direction = arg;
			}
		}

		return encode((memory & 3), direction);
	}

	private int calNumPherome(int dir, int fan, Cell player_cell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		Point left = extractVectorFromAngle(((dir - fan + 59) % 60) + 1);
		Point right = extractVectorFromAngle(((dir + fan - 1) % 60) + 1);
		int cnt = 0;

		Iterator<Pherome> pherome_it = nearby_pheromes.iterator();
		while (pherome_it.hasNext()) {
			Pherome cur = pherome_it.next();
			Point cur_vector = extractVectorFromAngle(p2pVector(player_cell, cur.getPosition()));
			if (pherome.player == player_cell.player && cross(left, cur_vector) * cross(right, cur_vector) < 0) ++cnt;
		}
		return cnt;
	}

	private int calNumItem(int dir, int fan, Cell player_cell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		Point left = extractVectorFromAngle(((dir - fan + 59) % 60) + 1);
		Point right = extractVectorFromAngle(((dir + fan - 1) % 60) + 1);
		int cnt = 0;

		Iterator<Cell> cell_it = nearby_cells.iterator();
		while (cell_it.hasNext()) {
			Cell cur = cell_it.next();
			Point cur_vector = extractVectorFromAngle(p2pVector(player_cell, cur.getPosition()));
			if (cross(left, cur_vector) * cross(right, cur_vector) < 0) ++cnt;
		}

		Iterator<Pherome> pherome_it = nearby_pheromes.iterator();
		while (pherome_it.hasNext()) {
			Pherome cur = pherome_it.next();
			Point cur_vector = extractVectorFromAngle(p2pVector(player_cell, cur.getPosition()));
			if (cross(left, cur_vector) * cross(right, cur_vector) < 0) ++cnt;
		}
		return cnt;
	}

	private Point p2pVector(Point p1, Point p2) {
		return new Point(p2.x - p1.x, p2.y - p1.y);
	}

	private int cross(Point p1, Point p2) {
		return (p1.x * p2.y - p1.y * p2.x);
	}

	private int encode(int role, int dir) {
		return ((dir << 2) | role);
	}

	/*
	double calCrowdSurround(Cel lplayer_cell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
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
	*/

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
		double theta = Math.toRadians( 6* (double)arg );
		double dx = Cell.move_dist * Math.cos(theta);
		double dy = Cell.move_dist * Math.sin(theta);
		return new Point(dx, dy);
	}
}
