package edu.csupomona.cs.cs420.project2;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Main class of the n-Queen solver program. This program will ask
 * for various information about the type of n-Queen problem that
 * needs to be solved, and then solve it for you.
 *
 * @author Collin Smith (collinsmith@csupomona.edu)
 */
public class Main {
	/**
	 * Random number generator used for randomizing the initial
	 * queen locations, as well as various other tasks throughout
	 * the program, such as choosing a random crossover point.
	 */
	private static final Random RAND = new Random();

	/**
	 * Main method executed at program start.
	 *
	 * @param args arguments passed to the program (none expected)
	 */
	public static void main(String[] args) {
		final Scanner SCAN = new Scanner(System.in);
		System.out.format("What size board do you want to use? ");
		final int N = SCAN.nextInt();
		System.out.format("How many random n-Queen problems do you want to generate? ");
		final int ITERATIONS = SCAN.nextInt();
		System.out.format("Do you run the steepest-ascent hill-climbing algorithm (y/n)? ");
		final boolean HILL_CLIMBING = SCAN.next("(y|n)").matches("y");
		if (HILL_CLIMBING) {
			System.out.format("Do you want to view the paths (y/n)? ");
		} else {
			System.out.format("How many elements do you want in each generation? ");
		}

		final boolean PRINT_PATHS = HILL_CLIMBING ? SCAN.next("(y|n)").matches("y") : false;
		final int GENERATION_ELEMENT_COUNT = HILL_CLIMBING ? 0 : SCAN.nextInt();

		if (!HILL_CLIMBING) {
			System.out.format("What do you want the mutation chance to be [0.0, 1.0]? ");
		}

		final double MUTATION_CHANCE = HILL_CLIMBING ? 0 : SCAN.nextDouble();
		SCAN.close();

		if (!HILL_CLIMBING) {
			Result r;
			for (int i = 0; i < ITERATIONS; i++) {
				List<Node> initialStates = new ArrayList(GENERATION_ELEMENT_COUNT);
				for (int j = 0; j < GENERATION_ELEMENT_COUNT; j++) {
					initialStates.add(new Node(generateRandomBoard(N)));
				}

				r = runGenetic(initialStates, MUTATION_CHANCE);
				System.out.format("Iteration %d:%n", i+1);
				System.out.println(r.TERMINAL_STATE);
				System.out.format("Elapsed Time: %dms%n", TimeUnit.NANOSECONDS.toMillis(r.END_TIME-r.START_TIME));
				System.out.format("Finished in %d generations.%n", r.NUM_MOVES);
			}
			
			return;
		}

		Result r;
		int[] board;
		if (PRINT_PATHS) {
			int i = 0;
			while (i < ITERATIONS) {
				board = generateRandomBoard(N);
				r = runHillClimbing(board, PRINT_PATHS);
				if (r.TERMINAL_STATE.COST != 0) {
					continue;
				}

				System.out.format("Iteration %d:%n", i+1);
				System.out.println(r.TERMINAL_STATE);
				System.out.format("Finished in %d moves.%n", r.NUM_MOVES);
				System.out.format("%d nodes generated.%n", r.SEARCH_COST);
				System.out.format("Number of attacking queens: %d%n", r.TERMINAL_STATE.COST);
				i++;
			}
		} else {
			Path file = Paths.get(".", "output", "output.txt");
			Charset charset = Charset.forName("US-ASCII");
			try (BufferedWriter writer = Files.newBufferedWriter(file, charset, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
				double count = 0;
				double moves = 0;
				double noSolutionMoves = 0;
				Set<Integer> solutions = new HashSet<>();
				System.out.format("%-8s %-16s %-16s %-16s %-16s%n", "run", "attacking queens", "moves", "search cost", "time");
				writer.write(String.format("%s\t%s\t%s\t%s\t%s%n", "run", "attacking_queens", "moves", "search_cost", "time"));
				for (int i = 0; i < ITERATIONS; i++) {
					board = generateRandomBoard(N);
					r = runHillClimbing(board, PRINT_PATHS);
					System.out.format("%-8d %-16d %-16d %-16d %-16d%n", i+1, r.TERMINAL_STATE.COST, r.NUM_MOVES, r.SEARCH_COST, TimeUnit.NANOSECONDS.toNanos(r.END_TIME-r.START_TIME));

					if (r.TERMINAL_STATE.COST == 0) {
						count++;
						moves += r.NUM_MOVES;
						solutions.add(i+1);
					} else {
						noSolutionMoves += r.NUM_MOVES;
					}

					writer.write(String.format("%d\t%d\t%d\t%d\t%d%n", i+1, r.TERMINAL_STATE.COST, r.NUM_MOVES, r.SEARCH_COST, TimeUnit.NANOSECONDS.toNanos(r.END_TIME-r.START_TIME)));
					writer.flush();
				}

				System.out.format("%.1f%% of problems were solved.%n", (count/ITERATIONS)*100);
				System.out.format("Avg. number of moves needed per solution: %.1f%n", moves/count);
				System.out.format("Avg. number of moves needed per no solution: %.1f%n", noSolutionMoves/(ITERATIONS-count));
				System.out.format("Solution Set (Iteration #): %s%n", Arrays.toString(solutions.toArray(new Integer[0])));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Generates a random n-queen board by placing a queen in the diagonal
	 * and then swapping each position randomly.
	 *
	 * @param N size of the board
	 *
	 * @return the randomly generated board
	 */
	private static int[] generateRandomBoard(final int N) {
		int[] BOARD = new int[N];
		for (int i = 0; i < BOARD.length; i++) {
			BOARD[i] = RAND.nextInt(BOARD.length);
		}

		return BOARD;
	}

	/**
	 * Runs a steepest-ascent hill-climbing algorithm with the given initial
	 * state and returning the resulting states.
	 *
	 * @param initialState initial board
	 * @param printPaths whether or not to print the paths
	 *
	 * @return a result object containing the results
	 */
	private static Result runHillClimbing(int[] initialState, boolean printPaths) {
		Node neighbor;
		List<Node> neighbors;
		Node current = new Node(initialState);
		if (current.COST == 0) {
			System.out.format("Search skipped, initial state is already a goal state.%n");
			System.out.println(current);
			System.out.println();

			return new Result(current, 0, 0, System.nanoTime());
		}

		int moves = 0;
		int searchCost = 0;
		final long START_TIME = System.nanoTime();
		while (true) {
			if (printPaths) {
				System.out.format("Move %d%n", moves);
				System.out.println(current);
				System.out.println();
			}

			neighbors = current.generateSuccessors();
			searchCost += neighbors.size();
			neighbor = neighbors.get(0);
			if (current.COST <= neighbor.COST) {
				return new Result(current, moves, searchCost, START_TIME);
			}

			current = neighbor;
			moves++;
		}
	}

	/**
	 * Runs a genetic algorithm on a list of given initial states and
	 * performs generations of genetic crossovers and mutations until
	 * a solution is found.
	 *
	 * @param initialStates list of initial states
	 *
	 * @return a result object containing statistical information
	 */
	private static Result runGenetic(List<Node> initialStates, double mutationChance) {
		Node[] crossover;
		Node highestFitness;
		List<Node> crossedOver = new ArrayList(initialStates.size());
		List<Node> bestOfCurrentGeneration;
		List<Node> nextGeneration = initialStates;

		int generations = 0;
		final long START_TIME = System.nanoTime();
		while (true) {
			bestOfCurrentGeneration = Genetic.select(nextGeneration);
			for (Node n : bestOfCurrentGeneration) {
				if (n.COST == 0) {
					return new Result(n, generations, 0, START_TIME);
				}
			}

			crossedOver.clear();
			highestFitness = bestOfCurrentGeneration.get(0);
			for (int i = 1; i < bestOfCurrentGeneration.size(); i++) {
				crossover = Genetic.crossover(highestFitness, bestOfCurrentGeneration.get(i));
				crossedOver.add(crossover[0]);
				crossedOver.add(crossover[1]);
			}

			nextGeneration.clear();
			for (Node n : crossedOver) {
				nextGeneration.add(Genetic.mutate(n, mutationChance));
			}

			generations++;
		}
	}

	/**
	 * This class contains methods associated with the genetic algorithm.
	 */
	private static class Genetic {
		/**
		 * Selects the best element and the next {@code n/2} best elements
		 * for breeding.
		 *
		 * @param pool the pool to perform the selection from
		 *
		 * @return a list containing the best elements
		 */
		static List<Node> select(List<Node> pool) {
			Collections.sort(pool, (Node n1, Node n2) -> {
				return n1.COST - n2.COST;
			});

			List<Node> selection = new ArrayList<>();
			for (int i = 0; i <= pool.size()>>1; i++) {
				selection.add(pool.get(i));
			}

			return selection;
		}

		/**
		 * Performs a crossover between two random nodes using a random point
		 *
		 * @param n1 first node to crossover
		 * @param n2 the other node to crossover n1 with
		 *
		 * @return an array containing the two crossed over nodes
		 */
		static Node[] crossover(Node n1, Node n2) {
			assert n1.BOARD.length == n2.BOARD.length;
			final int N = n1.BOARD.length;
			final int crossover = RAND.nextInt(N-1);

			int[] newBoard1 = new int[N];
			System.arraycopy(n1.BOARD, 0, newBoard1, 0, crossover);
			System.arraycopy(n2.BOARD, crossover, newBoard1, crossover, N-crossover);
			Node n12 = new Node(newBoard1);

			int[] newBoard2 = new int[N];
			System.arraycopy(n2.BOARD, 0, newBoard2, 0, crossover);
			System.arraycopy(n1.BOARD, crossover, newBoard2, crossover, N-crossover);
			Node n21 = new Node(newBoard2);

			return new Node[] { n12, n21 };
		}

		/**
		 * Performs a mutation operation on the given node with a given chance to mutate.
		 *
		 * @param n the node to mutate
		 * @param chance the chance between 0 and 1 to mutate
		 *
		 * @return the result of the mutation
		 */
		static Node mutate(Node n, double chance) {
			assert 0 <= chance && chance <= 1;
			if (chance < RAND.nextDouble()) {
				return n;
			}

			int[] mutated = Arrays.copyOf(n.BOARD, n.BOARD.length);
			final int POS = RAND.nextInt(mutated.length);
			mutated[POS] = RAND.nextInt(mutated.length);
			return new Node(mutated);
		}
	}

	/**
	 * This class contains various informational statisics resulting from
	 * an algoithm.
	 */
	private static final class Result {
		/**
		 * Final state of the algorithm
		 */
		final Node TERMINAL_STATE;

		/**
		 * Number of moves/steps the algorithm took
		 */
		final int NUM_MOVES;

		/**
		 * Cost of the algorithm
		 */
		final int SEARCH_COST;

		/**
		 * Initial start time
		 */
		final long START_TIME;

		/**
		 * End time, inferred when object created
		 */
		final long END_TIME;

		/**
		 * Constructs a Result object using the given information
		 * that the algorithm should have.
		 *
		 * @param terminalState final state of the algorithm
		 * @param numMoves number of moves the algorithm as taken
		 * @param searchCost the cost of the algorithm
		 * @param startTime time the algorithm started
		 */
		Result(Node terminalState, int numMoves, int searchCost, long startTime) {
			this.TERMINAL_STATE = terminalState;
			this.NUM_MOVES = numMoves;
			this.SEARCH_COST = searchCost;
			this.START_TIME = startTime;
			this.END_TIME = System.nanoTime();
		}
	}

	/**
	 * This class represents a node used within the n-queen problem
	 */
	private static class Node {
		/**
		 * Board containing the location of the queens
		 */
		final int BOARD[];

		/**
		 * "cost" of this board
		 */
		final int COST;

		/**
		 * Constructs a node using the specified board and inferring the cost.
		 *
		 * @param board array containing the location of the queens
		 */
		Node(int[] board) {
			this(board, countAttacking(board));
		}

		/**
		 * Constructs a node using the specified board with a specified cost.
		 *
		 * @param board array containing the location of the queens
		 * @param cost the cost of this node
		 */
		Node(int[] board, int cost) {
			this.BOARD = board;
			this.COST = cost;
		}

		List<Node> generateSuccessors() {
			int cost;
			int originalValue;
			int[] copy;
			int[] board = Arrays.copyOf(BOARD, BOARD.length);
			List<Node> successors = new ArrayList<>((BOARD.length*BOARD.length)-BOARD.length);
			for (int i = 0; i < board.length; i++) {
				originalValue = board[i];
				for (int j = 0; j < board.length; j++) {
					if (j == originalValue) {
						continue;
					}

					board[i] = j;
					cost = countAttacking(board);
					copy = Arrays.copyOf(board, board.length);
					successors.add(new Node(copy, cost));
				}

				board[i] = originalValue;
			}

			if (successors.size() != (BOARD.length*BOARD.length)-BOARD.length) {
				System.out.println("PROBLEM!!!!");
			}

			Collections.sort(successors, (Node n1, Node n2) -> {
				return n1.COST - n2.COST;
			});

			return successors;
		}

		/**
		 * Returns the number of queens attacking each other on a
		 * given board up to a certain index.
		 *
		 * @param BOARD the board to check
		 * @param i the maximum index to check
		 *
		 * @return the number of attacking queens
		 */
		static int countAttackingIth(final int[] BOARD, final int i) {
			int count = 0;
			final int CACHE = BOARD[i];
			for (int j = 0; j < i; j++) {
				if (BOARD[j] == CACHE) {
					count++;
				}

				if (BOARD[j]-CACHE == i-j) {
					count++;
				}

				if (CACHE-BOARD[j] == i-j) {
					count++;
				}
			}

			return count;
		}

		/**
		 * Returns the number of attacking queens up to a certain
		 * index.
		 *
		 * @param i the maximum index to check
		 *
		 * @return the number of attacking queens
		 */
		int countAttackingIth(final int i) {
			return countAttackingIth(BOARD, i);
		}

		/**
		 * Returns the number of attacking queens.
		 *
		 * @param BOARD the board o check
		 *
		 * @return the number of attacking queens
		 */
		static int countAttacking(final int[] BOARD) {
			int count = 0;
			for (int i = 0; i < BOARD.length; i++) {
				count += countAttackingIth(BOARD, i);
			}

			return count;
		}

		/**
		 * Returns the number of attacking queens.
		 *
		 * @retun the number of attacking queens wihin this node.
		 */
		int countAttacking() {
			return countAttacking(BOARD);
		}

		/**
		 * Returns a string representing this node.
		 *
		 * @return a string representing this node's board.
		 */
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < BOARD.length; i++) {
				for (int j = 0; j < BOARD.length; j++) {
					if (BOARD[i] == j) {
						sb.append('Q');
					} else {
						sb.append('.');
					}

					sb.append(' ');
				}

				sb.append('\n');
			}

			sb.deleteCharAt(sb.length()-1);
			return sb.toString();
		}
	}
}
