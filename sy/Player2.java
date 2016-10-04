package slather.g9;

import slather.sim.Cell;
import slather.sim.Point;
import slather.sim.Move;
import slather.sim.Pherome;
import java.util.*;


public class Player implements slather.sim.Player {

	private Random gen;

	private static int INF = (int)1e9;

	//==========functions==================================
	public void init(double d, int t, int side_length) {
		gen = new Random();
	}

	private Move Explore(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		int min_items = INF;
		int direction = 0;
		int jump = 10;
		for (int arg = 1; arg <= 180; arg += ((arg % 3 != 0)?(1):(jump))) {
			Point current_vector = extractVectorFromAngle(arg);
			if (collides(player_cell, current_vector, nearby_cells, nearby_pheromes)) continue;

			int cnt = calNumItem(arg, jump / 2, player_cell, nearby_cells, nearby_pheromes);
			if (cnt < min_items) {
				min_items = cnt;
				direction = arg;
			}
		}

		Point vector = extractVectorFromAngle(direction);
		return new Move(vector, memory);
	}

	private Move Invasive(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		int max_items = 0;
		int direction = 0;
		int jump = 10;
		for (int arg = 1; arg <= 180; arg += (arg % 3 != 0)?(1):(jump)) {
			Point current_vector = extractVectorFromAngle(arg);
			if (collides(player_cell, current_vecotr, nearby_cells, nearby_pheromes)) continue;

			int cnt = calNumItem(arg, jump / 2, player_cell, nearby_cells, nearby_pheromes);
			if (cnt > max_items) {
				max_items = cnt;
				direction = arg;
			}
		}

		Point vector = extractVectorFromAngle(direction);
		return new Move(vector, memory);
	}

	private int calNumItem(int dir, int fan, Cell player_cell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		Point left = extractVectorFromAngle(((dir - fan + 179) % 180) + 1);
		Point right = extractVectorFromAngle(((dir + fan - 1) % 180) + 1);
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

	public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		if (player_cell.getDiameter() >= 2 /*&& (calCrowdInDirection(player_cell, (int)memory, nearby_cells, nearby_pheromes) > COMF_RANGE)*/) // reproduce when it is not very crowd
			return new Move(true, (byte)-1, (byte)-1);

		if (memory > 0) { // follow previous direction in early stage, try least crowd direction in later
			if (calCrowdSurround(player_cell, nearby_cells, nearby_pheromes) < EARLY_STAGE) {
				Point vector = extractVectorFromAngle( (int)memory);
				// check for collisions
				if (!collides( player_cell, vector, nearby_cells, nearby_pheromes))
					return new Move(vector, memory);
			}
		}

		// otherwise, try random directions to go in until one doesn't collide
		double max_range = 0;
		int direction = 0;
		Point vector = new Point(0, 0);
		for (int arg = 1; arg <= 180; arg += 10) {
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
			return new Move(vector, (byte) direction);

		// if all tries fail, just chill in place
		return new Move(new Point(0,0), (byte)0);
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
