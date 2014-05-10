package edu.csupomona.cs.cs420.project2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

public class Main {
	private static final Random RAND = new Random();

	public static void main(String[] args) {
		final Scanner SCAN = new Scanner(System.in);
		System.out.format("What size board do you want to use? ");
		final int N = SCAN.nextInt();
		System.out.format("How many random n-Queen problems do you want to generate? ");
		final int ITERATIONS = SCAN.nextInt();
		SCAN.close();

		/*Set<Integer> solutions = Collections.synchronizedSet(new HashSet<>());
		final AtomicLong count = new AtomicLong();
		final AtomicInteger iteration = new AtomicInteger();

		final int availProcessors = Runtime.getRuntime().availableProcessors();
		Thread[] threads = new Thread[availProcessors];
		for (int j = 0; j < threads.length; j++) {
			threads[j] = new Thread(() -> {
				Node n;
				int[] board;
				int iterationCache;
				for (int i = 0; i < ITERATIONS/availProcessors; i++) {
					board = generateRandomBoard(N);
					n = hillClimb(board);
					iterationCache = iteration.incrementAndGet();
					synchronized (System.out) {
						System.out.format("Iteration %d:%n", iterationCache);
						System.out.println(n);
						System.out.format("Number of attacking queens: %d%n", n.COST);
					}

					if (n.COST == 1) {
						count.incrementAndGet();
						solutions.add(iterationCache);
					}
				}
			});
		}

		for (Thread t : threads) {
			try {
				t.start();
				t.join();
			} catch (InterruptedException e) {
			}
		}

		System.out.format("%.1f%% of problems were solved.%n", ((double)count.get()/ITERATIONS)*100);*/

		Set<Integer> solutions = new HashSet<>();
		double count = 0;

		Node n;
		int[] board;
		for (int i = 0; i < ITERATIONS; i++) {
			board = generateRandomBoard(N);
			n = hillClimb(board);
			System.out.format("Iteration %d:%n", i+1);
			System.out.println(n);
			System.out.format("Number of attacking queens: %d%n", n.COST);

			if (n.COST == 1) {
				count++;
				solutions.add(i+1);
			}
		}

		System.out.format("%.1f%% of problems were solved.%n", (count/ITERATIONS)*100);

		System.out.format("Solution Set: %s%n", Arrays.toString(solutions.toArray()));
	}

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

	private static void swap(int[] arr, int i, int j) {
		if (i == j) {
			return;
		}

		arr[i] ^= arr[j];
		arr[j] ^= arr[i];
		arr[i] ^= arr[j];
	}

	private static Node hillClimb(int[] initialState) {
		Node neighbor;
		List<Node> neighbors;
		Node current = new Node(initialState);
		int moves = 0;
		while (true) {
			moves++;
			neighbors = current.generateBestSuccessors();
			if (neighbors.isEmpty()) {
				System.out.format("Finished in %d moves.%n", moves);
				return current;
			}

			neighbor = neighbors.get(RAND.nextInt(neighbors.size()));
			// I don't need this because generageBestSuccessors() guaranees that the cost of all are better than current
			/*if (current.COST <= neighbor.COST) {
				System.out.format("Finished in %d moves.%n", moves);
				return current;
			}*/

			current = neighbor;
		}
	}

	private static class Genetic {
		void crossover(Node n1, Node n2) {
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
		}
	}

	private static final class Node {
		final int BOARD[];
		final int COST;

		Node(int[] board) {
			this(board, countAttacking(board));
		}

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

		List<Node> generateBestSuccessors() {
			int cost;
			int originalValue;
			int minCost = COST-1;
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

		int countAttackingIth(final int i) {
			return countAttackingIth(BOARD, i);
		}

		static int countAttacking(final int[] BOARD) {
			int count = 0;
			for (int i = 0; i < BOARD.length; i++) {
				count += countAttackingIth(BOARD, i);
			}

			return count;
		}

		int countAttacking() {
			return countAttacking(BOARD);
		}

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
