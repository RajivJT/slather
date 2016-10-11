package slather.test;

import slather.sim.Cell;
import slather.sim.Point;
import slather.sim.Move;
import slather.sim.Pherome;
import java.util.*;

// find largest free angle, by sorting enemy cells and pheromes

public class Player implements slather.sim.Player {

	private Random gen;

	//=========parameters==================================
	private static double eps = 1e-9;
	private static double EARLY_STAGE = 0;
	private static double LATE_STAGE = 4. / 5.;
	private static double COMF_RANGE = 1;

	//==========functions==================================
	public void init(double d, int t, int side_length) {
		gen = new Random();
	}

	public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		// produce when it is possible
		if (player_cell.getDiameter() >= 2) {
			byte dir = reverse(memory);
			return new Move(true, memory, dir);
		}

		double crowdGra = calCrowdSurround(player_cell, nearby_cells, nearby_pheromes);
		// early stage
		if (crowdGra < EARLY_STAGE) {
			return playEarlyStage(player_cell, memory, nearby_cells, nearby_pheromes);
		}

		// late stage
		if (crowdGra > EARLY_STAGE - eps) {
			return playLateStage(player_cell, memory, nearby_cells, nearby_pheromes);
		}

		// default strategy
		return playDefault(player_cell, memory, nearby_cells, nearby_pheromes);
	}

	public class Circle {
		public double x, y;
		public double d;

		public Circle(Cell cell) {
			x = cell.getPosition().x;
			y = cell.getPosition().y;
			d = cell.getDiameter();
		}
	}

	public class Enemy {
		public Circle enemy;
		public Circle self;

		public Point vector;

		public Enemy(Cell player_cell, Cell enemy_cell) {
			enemy = new Circle(enemy_cell);
			self = new Circle(player_cell);

			vector = new Point(enemy_cell.getPosition().x - player_cell.getPosition().x, enemy_cell.getPosition().y - player_cell.getPosition().y);
		}
	}

	class EnemyComparator implements Comparator<Enemy> {
		@Override
		public int compare(Enemy e1, Enemy e2) {
			return (cross(e1.vector, e2.vector) > 0);
		}
	}

	private double cross(Point p1, Point p2) {
		return (p1.x * p2.y - p1.y * p2.x);
	}

	private Move playDefault(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		ArrayList<Enemy> enemy = new ArrayList<Enemy>();
		for (Cell cell : nearby_cells) {
			if (cell.player != player_cell.player) enemy.add(new Enemy(player_cell, cell));
		}
		for (Pherome p : nearby_pheromes) {
			if (p.player != player_cell.player) enemy.add(new Enemy(player_cell, Pherome));
		}
		Collections.sort(enemy, new EnemyComparator());

		double gap = 0.;
		Enemy e1 = null, e2 = null;
		for (int i = 0; i < enemy.size(); ++i) {
			Enemy tmp1 = enemy.get(i);
			Enemy tmp2 = ((i == enemy.size() - 1)?(enemy.get(0)):(enemy.get(i + 1)));
			double c = cross(tmp1.vector, tmp2.vector);
			if (c > gap) {
				gap = c;
				e1 = tmp1; e2 = tmp2;
			}
		}

		Point dir = getMidVector(e1.vector, e2.vector);
		byte angle = getAngleFromVector(dir);
		if (!collides(player_cell, dir, nearby_cells, nearby_pheromes)) return new Move(dir, angle);

		for (byte angle = 0; angle < 180; angle += 2) {
			Point dir = extractVectorFromAngle(angle);
			if (!collides(player_cell, dir, nearby_cells, nearby_pheromes)) return new Move(dir, angle);
		}

		// chill
		int ran = gen.nextInt(180);
		return new Move(new Point(0, 0), (byte)ran);
	}

	private Move playEarlyStage(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
	}

	private Move playLateStage(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
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

	private byte getAngleFromVector(Point vector) {
		double theta = Math.atan2(vector.y, vector.x);
		return (byte)(theta / 2.);
	}

	private Point getMidVector(Point v1, Point v2) {
	}

	private byte reverse(byte angle) {
		if (angle < 90) return angle + 90;
		return angle - 90;
	}

}
