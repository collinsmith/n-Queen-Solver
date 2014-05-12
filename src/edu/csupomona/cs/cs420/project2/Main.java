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
import java.util.Comparator;
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
		System.out.format("Do you run the hill-climbing algorithm (y/n)? ");
		final boolean HILL_CLIMBING = SCAN.next("(y|n)").matches("y");
		if (HILL_CLIMBING) {
			System.out.format("Do you want to view the paths (y/n)? ");
		} else {
			System.out.format("How many elements do you want in each generation? ");
		}
		
		final boolean PRINT_PATHS = HILL_CLIMBING ? SCAN.next("(y|n)").matches("y") : false;
		final int GENERATION_ELEMENT_COUNT = HILL_CLIMBING ? 0 : SCAN.nextInt();
		SCAN.close();

		if (!HILL_CLIMBING) {
			Result r;
			for (int i = 0; i < ITERATIONS; i++) {
				List<Node> initialStates = new ArrayList(GENERATION_ELEMENT_COUNT);
				for (int j = 0; j < GENERATION_ELEMENT_COUNT; j++) {
					initialStates.add(new Node(generateRandomBoard(N)));
				}

				r = runGenetic(initialStates);
				System.out.format("Iteration %d:%n", i+1);
				System.out.println(r.TERMINAL_STATE);
				System.out.format("Elapsed Time: %dms%n", TimeUnit.NANOSECONDS.toMillis(r.END_TIME-r.START_TIME));
				System.out.format("Finished in %d generations.%n", r.NUM_MOVES);
				System.out.format("Number of attacking queens: %d%n", r.TERMINAL_STATE.COST);
			}
			return;
		}
		
		Set<Integer> solutions = new HashSet<>();
		double count = 0;
		double moves = 0;
		double noSolutionMoves = 0;

		Path file = Paths.get(".", "output", "output.txt");
		Charset charset = Charset.forName("US-ASCII");
		try (BufferedWriter writer = Files.newBufferedWriter(file, charset, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			if (!PRINT_PATHS) {
				System.out.format("%-8s %-8s %-8s %-16s%n", "#", "queens", "moves", "run time");
			}

			writer.write(String.format("%s\t%s\t%s\t%s%n", "#", "queens", "moves", "run time"));

			Result r;
			int[] board;
			for (int i = 0; i < ITERATIONS; i++) {
				board = generateRandomBoard(N);
				r = runHillClimbing(board, PRINT_PATHS);
				if (PRINT_PATHS) {
					System.out.format("Iteration %d:%n", i+1);
					System.out.println(r.TERMINAL_STATE);
					System.out.format("Finished in %d moves.%n", r.NUM_MOVES);
					System.out.format("Number of attacking queens: %d%n", r.TERMINAL_STATE.COST);
				} else {
					System.out.format("%-8d %-8d %-8d %-16d%n", i+1, r.TERMINAL_STATE.COST, r.NUM_MOVES, TimeUnit.NANOSECONDS.toNanos(r.END_TIME-r.START_TIME));
				}

				if (r.TERMINAL_STATE.COST == 1) {
					count++;
					moves += r.NUM_MOVES;
					solutions.add(i+1);
				} else {
					noSolutionMoves += r.NUM_MOVES;
				}

				writer.write(String.format("%d\t%d\t%d\t%d%n", i+1, r.TERMINAL_STATE.COST, r.NUM_MOVES, TimeUnit.NANOSECONDS.toNanos(r.END_TIME-r.START_TIME)));
				writer.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}


		System.out.format("%.1f%% of problems were solved.%n", (count/ITERATIONS)*100);
		System.out.format("Avg. number of moves needed per solution: %.1f%n", moves/count);
		System.out.format("Avg. number of moves needed per no solution: %.1f%n", noSolutionMoves/(ITERATIONS-count));
		System.out.format("Solution Set (Iteration #): %s%n", Arrays.toString(solutions.toArray(new Integer[0])));
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
			BOARD[i] = i;
		}

		int randomIndex;
		for (int i = 0; i < BOARD.length; i++) {
			randomIndex = RAND.nextInt(BOARD.length);
			swap(BOARD, i, randomIndex);
		}

		return BOARD;
	}

	/**
	 * Swaps two indeces in the array.
	 * 
	 * @param arr the array to swap
	 * @param i the first index to swap
	 * @param j the index to swap it with
	 */
	private static void swap(int[] arr, int i, int j) {
		if (i == j) {
			return;
		}

		arr[i] ^= arr[j];
		arr[j] ^= arr[i];
		arr[i] ^= arr[j];
	}

	/**
	 * Runs a steepes hill-climbing algorithm with the given initial
	 * state and returning the resulting states.
	 * 
	 * @param initialState initial board
	 * @param printPaths whether or not to print the paths
	 * 
	 * @return a result object containing the results
	 */
	private static Result runHillClimbing(int[] initialState, boolean printPaths) {
		Node neighbor;
		List<Node> betterSuccessors;
		Node current = new Node(initialState);

		int moves = 0;
		final long START_TIME = System.nanoTime();
		while (true) {
			if (printPaths) {
				System.out.format("Move %d%n", moves);
				System.out.println(current);
				System.out.println();
			}

			betterSuccessors = current.generateBetterSuccessors();
			if (betterSuccessors.isEmpty()) {
				// peak reached, not a guaranteed solution
				return new Result(current, moves, START_TIME);
			}

			neighbor = betterSuccessors.get(RAND.nextInt(betterSuccessors.size()));
			current = neighbor;
			if (current.COST == 1) {
				// we've found a solution
				return new Result(current, moves, START_TIME);
			}

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
	private static Result runGenetic(List<Node> initialStates) {
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
				if (n.COST == 1) {
					return new Result(n, generations, START_TIME);
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
				nextGeneration.add(Genetic.mutate(n));
			}
			
			generations++;
		}
	}
	
	/**
	 * This class contains methods associated with the genetic alogorithm.
	 */
	private static class Genetic {
		/**
		 * Comparator which can determine which nodes are better than others.
		 */
		static final Comparator<Node> FITNESS_FUNCTION = (Node n1, Node n2) -> {
			return n1.COST - n2.COST;
		};
		
		/**
		 * Selects the best {@code n/2 + 1} elements
		 * 
		 * @param currentGeneration the generation to perform the selection from
		 * 
		 * @return a list containing the best elements according o {@link #FITNESS_FUNCTION}
		 */
		static List<Node> select(List<Node> currentGeneration) {
			assert (currentGeneration.size()&1) == 0 : "Generation sizes should be even!";
			Collections.sort(currentGeneration, FITNESS_FUNCTION);
			List<Node> nextGeneration = new ArrayList<>();
			for (int i = 0; i <= currentGeneration.size()>>1; i++) {
				nextGeneration.add(currentGeneration.get(i));
			}
			
			return nextGeneration;
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
			final int crossover = RAND.nextInt(N);

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
		 * Performs a mutation operation on the given node with {@code P=1}
		 * 
		 * @param n the node to mutate
		 * 
		 * @return the result of the mutation
		 */
		static Node mutate(Node n) {
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
		 * @param startTime time the algorithm started
		 */
		Result(Node terminalState, int numMoves, long startTime) {
			this.TERMINAL_STATE = terminalState;
			this.NUM_MOVES = numMoves;
			this.START_TIME = startTime;
			this.END_TIME = System.nanoTime();
		}
	}

	/**
	 * This class represents a node used within the n-queen problem
	 */
	private static final class Node {
		/**
		 * Board contianing the location of the queens
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

		Node generateSuccessor() {
			final int[] NEW_BOARD = Arrays.copyOf(BOARD, BOARD.length);
			final int POS = RAND.nextInt(BOARD.length);
			if (RAND.nextBoolean()) {
				NEW_BOARD[POS]++;
				if (NEW_BOARD.length == NEW_BOARD[POS]) {
					NEW_BOARD[POS] = 0;
				}
			} else {
				NEW_BOARD[POS]--;
				if (NEW_BOARD[POS] < 0) {
					NEW_BOARD[POS] = NEW_BOARD.length-1;
				}
			}

			return new Node(NEW_BOARD);
		}

		/**
		 * Returns a list containing successors which have a lower cost
		 * than this node.
		 * 
		 * @return list containing successors which are better
		 */
		List<Node> generateBetterSuccessors() {
			if (COST == 1) {
				return Collections.EMPTY_LIST;
			}

			int cost;
			int originalValue;
			int minCost = Math.max(COST-1, 1);
			int[] copy;
			int[] board = Arrays.copyOf(BOARD, BOARD.length);
			List<Node> bestSuccessors = new ArrayList<>();
			for (int i = 0; i < board.length; i++) {
				originalValue = board[i];
				for (int j = 0; j < board.length; j++) {
					if (j == originalValue) {
						continue;
					}

					board[i] = j;
					cost = countAttacking(board);
					if (cost <= minCost) {
						if (cost <= 0) {
							continue;
						}

						if (cost == minCost) {
							minCost = cost;
							bestSuccessors.clear();
						}

						copy = Arrays.copyOf(board, board.length);
						bestSuccessors.add(new Node(copy, cost));
					}
				}

				board[i] = originalValue;
			}

			return bestSuccessors;
		}

		/**
		 * Returns the number of queens attacking eachother on a
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
				} else if (BOARD[j]-CACHE == i-j) {
					count++;
				} else if (CACHE-BOARD[j] == i-j) {
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
