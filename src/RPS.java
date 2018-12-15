import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CyclicBarrier;
import java.util.Random;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class RPS 
{	
	// Player's current move
	// 1 = Rock, 2 = Paper, 3 = Scissors
	public static int currentMove;
	public static int numTurns;
	public static CyclicBarrier barrier;
	
	public static void main( String args[] )
	{
		numTurns = Integer.parseInt( args[0] );
		currentMove = 0;
		barrier = new CyclicBarrier( 5 );
		
		int port = 35655, playerPortA = 0, playerPortB = 0;
		boolean connectedPlayerA = false, connectedPlayerB = false;
		
		
		// Try to open ports at 35655-35657 for three-player game
		ServerSocket socket = openPort( port );		
		
		while ( socket == null )
		{
			port++;
			socket = openPort( port );
		}
		
		switch ( port )
		{
		case 35655:
			playerPortA = 35656;
			playerPortB = 35657;
			break;
		case 35656:
			playerPortA = 35655;
			playerPortB = 35657;
			break;
		case 35657:
			playerPortA = 35655;
			playerPortB = 35656;
			break;
		}
		
		// Start accepting connections
		Thread t = new Thread( new RPS_Server( socket ) );
		t.start();
		
		// Keep trying to connect to the other players
		while( !connectedPlayerA && !connectedPlayerB )
		{
			if ( !connectedPlayerA )
				connectedPlayerA = connect( playerPortA );
			
			if ( !connectedPlayerB )
				connectedPlayerB = connect( playerPortB );
		}
		
		try 
		{
			t.join();
		} 
		catch ( InterruptedException e ) 
		{
			e.printStackTrace();
		}
	}
	
	// Check if port is open. Return socket if true
	private static ServerSocket openPort( int port )
	{
		ServerSocket socket;		
		try 
		{
			socket = new ServerSocket( port );
		} 
		catch ( IOException e ) 
		{
			return null;
		}		
		return socket;
	}
	
	private static boolean connect( int port )
	{
		Socket socket;
		try 
		{
			socket = new Socket( "localhost", port );
		} 
		catch ( IOException e ) 
		{
			return false;
		}		

		Thread t = new Thread( new RPS_Client( socket  ) );
		t.start();
		
		return true;
	}
}
	
class RPS_Server implements Runnable
{	
	private ServerSocket serverSocket;
	private int playerScores[];
	
	public RPS_Server( ServerSocket socket )
	{
		serverSocket = socket;
	}
	
	public void run()
	{	
		playerScores = new int[3];
		
		Random rand = new Random();
		ConnectionHandler players[] = new ConnectionHandler[2];
		int playerMoves[] = new int[2];
		int connections = 0;
		
		while ( connections < 2 )
		{
			System.out.println( "Waiting for " + ( 2 - connections ) + " more players..." );
			
			try 
			{
				players[connections] = new ConnectionHandler( serverSocket.accept() );
			} 
			catch ( IOException e ) 
			{
				e.printStackTrace();
			}
			
			connections++;
		}
		
		System.out.println( "All players have connected! Starting game...");
		Thread t0 = new Thread( players[0] );
		Thread t1 = new Thread( players[1] );
		t0.start();
		t1.start();
		
		for ( int i = 0; i < RPS.numTurns; i++ )
		{
			try 
			{
				RPS.barrier.await();
			} 
			catch ( Exception e ) 
			{
				e.printStackTrace();
			}
			
			System.out.println( "Game " + i + ":\n" );
			
			// Random move
			RPS.currentMove = rand.nextInt( 3 ) + 1;
			
			for ( int j = 0; j < 2; j++ )
			{
				while ( playerMoves[j] == 0 )
				{
					playerMoves[j] = Integer.parseInt( players[j].getPlayerMove() );
				}
			}
			
			// Calculate scores for round
			calculateRoundScores( playerMoves[0], playerMoves[1] );
			
			// Print the moves of each player and the current scores
			System.out.println( "MOVES: YOU: " + parsePlayerMove( RPS.currentMove ) + " | PLAYER A: " + parsePlayerMove( playerMoves[0] ) + " | PLAYER B: " + parsePlayerMove( playerMoves[1] ) );
			System.out.println( "SCORE: YOU: " + playerScores[0] + " | PLAYER A: " + playerScores[1] + " | PLAYER B: " + playerScores[2] );
			
			// Clean out player moves
			playerMoves[0] = 0;
			playerMoves[1] = 0;
			
			
			try 
			{
				// Pause between turns
				Thread.sleep( 500 );
			} 
			catch ( InterruptedException e ) 
			{
				e.printStackTrace();
			}
		}
		
		try 
		{
			serverSocket.close();
		} 
		catch ( IOException e ) 
		{
			e.printStackTrace();
		}
	}
	
	private void calculateRoundScores( int playerMoveOne, int playerMoveTwo )
	{
		if ( didPlayerWin( RPS.currentMove, playerMoveOne )  )
		{
			if ( didPlayerWin( RPS.currentMove, playerMoveTwo ) )
			{
				// We beat both other players
				playerScores[0] += 2;
			}
			else if ( didPlayerWin( playerMoveTwo, playerMoveOne ) )
			{
				// Player one lost to both other players
				playerScores[0]++;
				playerScores[2]++;
			}
		}
		else if ( didPlayerWin( playerMoveOne, playerMoveTwo ) )
		{
			{
				if ( didPlayerWin( playerMoveOne, RPS.currentMove ) )
				{
					// Player one beat both other players
					playerScores[1] += 2;
				}
				else if ( didPlayerWin( RPS.currentMove, playerMoveTwo ) )
				{
					// Player two lost to both other players
					playerScores[0] += 1;
					playerScores[1] += 1;
				}
			}
		}
		else if ( didPlayerWin( playerMoveTwo, RPS.currentMove ) )
		{
			if ( didPlayerWin( playerMoveTwo, playerMoveOne ) )
			{
				// Player two beat both other players
				playerScores[2] += 2;
			}
			else if ( didPlayerWin( playerMoveOne, RPS.currentMove ) )
			{
				// We lost to both other players
				playerScores[1]++;
				playerScores[2]++;
			}
		}
	}
	
	private boolean didPlayerWin( int playerMove, int playerMoveOther )
	{
		switch ( playerMove )
		{
		case 1:
			return ( playerMoveOther == 3 ) ? true : false;
		case 2:
			return ( playerMoveOther == 1 ) ? true : false;
		case 3:
			return ( playerMoveOther == 2 ) ? true : false;
		}
		
		return false;
	}
	
	private String parsePlayerMove( int playerMove )
	{
		switch ( playerMove )
		{
		case 1:
			return "Rock";
		case 2:
			return "Paper";
		case 3:
			return "Scissors";
		}
		
		return "";
	}
}

// Handles a single player connection
class ConnectionHandler implements Runnable
{
	private Socket clientSocket;
	private BufferedReader in;
	
	public ConnectionHandler( Socket socket )
	{
		clientSocket = socket;	
	}
	
	public void run()
	{
		try
		{
	        in = new BufferedReader( new InputStreamReader( clientSocket.getInputStream() ) );

        	// Keep running until we play every round
			for ( int i = 0; i < RPS.numTurns; i++ ) { RPS.barrier.await(); }
			
			// Give server time to finish up during last iteration
			Thread.sleep( 1000 );

	        // Close connections
	        clientSocket.close();
	        in.close();
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
	}
	
	public String getPlayerMove()
	{
		String msg = "";
		
		try 
		{
			if ( in != null )
			{
				msg = in.readLine();
			}
		} 
		catch ( IOException e ) 
		{
			e.printStackTrace();
		}		
		return msg;
	}
}

// Sends inputs to other player
class RPS_Client implements Runnable
{
	private Socket clientSocket;
	private PrintWriter out;
	
	public RPS_Client( Socket socket )
	{
		clientSocket = socket;
	}
	
	public void run() 
	{
		try
		{
			out = new PrintWriter( clientSocket.getOutputStream(), true );
			
			for ( int i = 0; i < RPS.numTurns; i++ )
			{
				RPS.barrier.await();
				
				// Give the server thread time to generate a new move
				Thread.sleep( 100 );	
				
				out.println( RPS.currentMove );	        
			}

	        // Close connections
	        clientSocket.close();
	        out.close();
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
	}
}