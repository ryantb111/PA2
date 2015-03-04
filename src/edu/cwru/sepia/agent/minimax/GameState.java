package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;
import edu.cwru.sepia.util.DistanceMetrics;

import java.util.*;
import java.util.Map.Entry;

/**
 * This class stores all of the information the agent
 * needs to know about the state of the game. For example this
 * might include things like footmen HP and positions.
 *
 * Add any information or methods you would like to this class,
 * but do not delete or change the signatures of the provided methods.
 */
public class GameState {
	
	List<GameStateUnit> playerUnits;
	List<GameStateUnit> enemyUnits;
	List<ResourceView> resourceViews;
	
	int maxX;
	int maxY;
	
//	public State.StateView state;
    /**
     * You will implement this constructor. It will
     * extract all of the needed state information from the built in
     * SEPIA state view.
     *
     * You may find the following state methods useful:
     *
     * state.getXExtent() and state.getYExtent(): get the map dimensions
     * state.getAllResourceIDs(): returns all of the obstacles in the map
     * state.getResourceNode(Integer resourceID): Return a ResourceView for the given ID
     *
     * For a given ResourceView you can query the position using
     * resource.getXPosition() and resource.getYPosition()
     *
     * For a given unit you will need to find the attack damage, range and max HP
     * unitView.getTemplateView().getRange(): This gives you the attack range
     * unitView.getTemplateView().getBasicAttack(): The amount of damage this unit deals
     * unitView.getTemplateView().getBaseHealth(): The maximum amount of health of this unit
     *
     * @param state Current state of the episode
     */
    public GameState(State.StateView state) {
    	List<UnitView> playerUnitViews = state.getUnits(0); // 0 = the player ID, should be a constant declared somewhere...
    	playerUnits = new ArrayList<GameState.GameStateUnit>();
    	for(UnitView unitView : playerUnitViews) {
    		playerUnits.add(new GameStateUnit(unitView));
    	}
    	
    	List<UnitView> enemyUnitViews = state.getUnits(1);// 1 = the enemy ID, should be a constant declared somewhere... 
    	// It would be really nice to be able to get all units for non-player agents, rather than just for players 0 and 1.
    	// Might be able to play with removeIf(Predicate...) to accomplish this generality.
    	
    	enemyUnits = new ArrayList<GameState.GameStateUnit>();
    	for(UnitView unitView : enemyUnitViews) {
    		enemyUnits.add(new GameStateUnit(unitView));
    	}
    	
    	resourceViews = state.getAllResourceNodes();
    	
    	maxX = state.getXExtent()-1;
    	maxY = state.getYExtent()-1;
    }
	
	public GameState(int maxX, int maxY, List<ResourceView> resources) {
		this.resourceViews = resources;
		this.maxX = maxX;
		this.maxY = maxY;
	}

    /**
     * You will implement this function.
     *
     * You should use weighted linear combination of features.
     * The features may be primitives from the state (such as hp of a unit)
     * or they may be higher level summaries of information from the state such
     * as distance to a specific location. Come up with whatever features you think
     * are useful and weight them appropriately.
     *
     * It is recommended that you start simple until you have your algorithm working. Then watch
     * your agent play and try to add features that correct mistakes it makes. However, remember that
     * your features should be as fast as possible to compute. If the features are slow then you will be
     * able to do less plys in a turn.
     *
     * Add a good comment about what is in your utility and why you chose those features.
     *
     * @return The weighted linear combination of the features
     */
    public double getUtility() {
    	double utility = 0;
    	
    	double totalPlayerHitpoints = 0;
    	double totalEnemyHitpoints = 0;
    	double enemyCorneredMetric = 0;
    	double playerBehindCoverMetric = 0;
    	
    	
    	for(GameStateUnit gameStateUnit : playerUnits) {
    		totalPlayerHitpoints += gameStateUnit.getHitpoints();

    		// Player behind cover metric is a little complicated.
    		// I'll do it later!
    		
    	}
    	
    	for(GameStateUnit gameStateUnit : enemyUnits) {
    		totalEnemyHitpoints += gameStateUnit.getHitpoints();
    		enemyCorneredMetric += Math.pow(blockedDirections(gameStateUnit),2);
    	}
    	
        return utility;
    }

    private int blockedDirections(GameStateUnit gameStateUnit) {
    	
    	if(gameStateUnit.isDead()) {
    		return 0;
    	}
    	
    	int blockedDirections = 0;
    	
    	int id = gameStateUnit.getOriginalUnit().getID();
    	
    	direction_loop:
    	for(Direction potentialDirection : allowedDirections) {
    		// compute the ending x,y coordinate
    		int x = gameStateUnit.getxLoc()+potentialDirection.xComponent();
    		int y = gameStateUnit.getyLoc()+potentialDirection.yComponent();
    		
    		// Figure out if anything in our objects has this component already.
    		for(GameStateUnit otherUnit : playerUnits) {
    			if(otherUnit.getxLoc() == x && otherUnit.getyLoc() == y) {
    				if(otherUnit.getOriginalUnit().getID() != id) {
    					blockedDirections++;
    					continue direction_loop;
    				}
    			}
    		}
    		
    		for(GameStateUnit otherUnit : enemyUnits) {
    			if(otherUnit.getxLoc() == x && otherUnit.getyLoc() == y) {
    				if(otherUnit.getOriginalUnit().getID() != id) {
    					blockedDirections++;
    					continue direction_loop;
    				}
    			}
    		}
    		
    		for(ResourceView resource : resourceViews) {
    			if(resource.getXPosition() == x && resource.getYPosition() == y) {
    				blockedDirections++;
    				continue direction_loop;
    			}
    		}
    	}
		return blockedDirections;
	}

	/**
     * You will implement this function.
     *
     * This will return a list of GameStateChild objects. You will generate all of the possible
     * actions in a step and then determine the resulting game state from that action. These are your GameStateChildren.
     *
     * You may find it useful to iterate over all the different directions in SEPIA.
     *
     * for(Direction direction : Directions.values())
     *
     * To get the resulting position from a move in that direction you can do the following
     * x += direction.xComponent()
     * y += direction.yComponent()
     *
     * @return All possible actions and their associated resulting game state
     */
    public List<GameStateChild> getChildren() {
    	List<GameStateChild> result = new ArrayList<GameStateChild>();
    	
    	// All combinations of (unit->action) mappings
    	List<Map<Integer, Action>> allUnitActionMaps = new ArrayList<Map<Integer,Action>>();
    	
    	List<GameStateUnit> allUnits = new ArrayList<GameState.GameStateUnit>();
    	
    	allUnits.addAll(playerUnits);// SLOOOOW
    	allUnits.addAll(enemyUnits); // SLOOOOW
    	
    	for(GameStateUnit gameStateUnit : allUnits) {
    		
    		List<Map<Integer, Action>> derivedUnitActionMaps = new ArrayList<Map<Integer,Action>>();
    		for(Map<Integer, Action> existingActionMap : allUnitActionMaps) {
    			
    			// We need to make multiple variations of every existing action map
    			for(Action possibleAction : getPossibleActions(gameStateUnit)) {
    				// Copy the existing map into a new object
    				Map<Integer, Action> newActionMap = new HashMap<Integer, Action>();
    				for(Entry<Integer, Action> entry : existingActionMap.entrySet()) {
    					newActionMap.put(entry.getKey(), entry.getValue());
    				}
    				// Modify the new object with the action selected for the new unit -> action map.
    				newActionMap.put(gameStateUnit.getOriginalUnit().getID(), possibleAction); 
    				
    				// Store the new unit->action map;
    				derivedUnitActionMaps.add(newActionMap);
    			}
    		}
    		allUnitActionMaps = derivedUnitActionMaps;
    	}
    	for(Map<Integer, Action> unitToActionMap : allUnitActionMaps) {
    		result.add(new GameStateChild(unitToActionMap, GameStateUtility.computeGameState(unitToActionMap, this)));
    	}
    	return result;
    }

    private static Direction[] allowedDirections = new Direction[] {
    	Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };
    
	private List<Action> getPossibleActions(GameStateUnit gameStateUnit) {
		List<Action> allPossibleActions = new ArrayList<Action>();
		
		Integer unitId = gameStateUnit.getOriginalUnit().getID();
		
		for(Direction direction : allowedDirections) {
			allPossibleActions.add(Action.createPrimitiveMove(unitId, direction));
		}
		
		// Only consider attacking enemy units.
		// Only consider attacking units within range.
		List<GameStateUnit> unitsToCheck = (gameStateUnit.getOriginalUnit().getTemplateView().getPlayer()==0) ? enemyUnits : playerUnits;
		for(GameStateUnit unitToCheck : unitsToCheck) {
			if(DistanceMetrics.euclideanDistance(
					gameStateUnit.getxLoc(), 
					gameStateUnit.getyLoc(),
					unitToCheck.getxLoc(),
					unitToCheck.getyLoc()
				) <= gameStateUnit.getOriginalUnit().getTemplateView().getRange()) 
			{
				allPossibleActions.add(Action.createPrimitiveAttack(unitId, unitToCheck.getOriginalUnit().getID()));	
			}
		}
		return allPossibleActions;
	}

	public List<GameStateUnit> getPlayerUnits() {
		return playerUnits;
	}

	public void setPlayerUnits(List<GameStateUnit> playerUnits) {
		this.playerUnits = playerUnits;
	}

	public List<GameStateUnit> getEnemyUnits() {
		return enemyUnits;
	}

	public void setEnemyUnits(List<GameStateUnit> enemyUnits) {
		this.enemyUnits = enemyUnits;
	}

	public List<ResourceView> getResourceViews() {
		return resourceViews;
	}

	public void setResourceViews(List<ResourceView> resourceViews) {
		this.resourceViews = resourceViews;
	}

	public int getMaxX() {
		return maxX;
	}

	public void setMaxX(int maxX) {
		this.maxX = maxX;
	}

	public int getMaxY() {
		return maxY;
	}

	public void setMaxY(int maxY) {
		this.maxY = maxY;
	}

	public boolean getGameOver() {
		if(playerUnits.isEmpty() || enemyUnits.isEmpty()) {
			return true;
		}
		boolean existsAlivePlayerUnit = false;
		boolean existsAliveEnemyUnit = false;
		for(GameStateUnit playerUnit : playerUnits) {
			if(!playerUnit.isDead()) {
				existsAlivePlayerUnit = true;
				break;
			}
		}
		if(!existsAlivePlayerUnit) {
			return true;
		}
		
		for(GameStateUnit enemyUnit : enemyUnits) {
			if(!enemyUnit.isDead()) {
				existsAliveEnemyUnit = true;
				break;
			}
		}
		if(!existsAliveEnemyUnit) {
			return true;
		}
		return false;
		
	}
	
	
	public static class GameStateUnit{
		private UnitView originalUnit;
		private int xLoc;
		private int yLoc;
		private float hitpoints;
		
		public GameStateUnit(UnitView copyFrom) {
			originalUnit = copyFrom;
			xLoc = originalUnit.getXPosition();
			yLoc = originalUnit.getYPosition();
			hitpoints = originalUnit.getHP();
			
			
		}
		
		
		public GameStateUnit(GameStateUnit copyFrom) {
			originalUnit = copyFrom.getOriginalUnit();
			xLoc = copyFrom.getxLoc();
			yLoc = copyFrom.getyLoc();
			hitpoints = copyFrom.getHitpoints();
		}
		
		public UnitView getOriginalUnit() {
			return originalUnit;
		}
		public void setOriginalUnit(UnitView originalUnit) {
			this.originalUnit = originalUnit;
		}
		public int getxLoc() {
			return xLoc;
		}
		public void setxLoc(int xLoc) {
			this.xLoc = xLoc;
		}
		public int getyLoc() {
			return yLoc;
		}
		public void setyLoc(int yLoc) {
			this.yLoc = yLoc;
		}
		public float getHitpoints() {
			return hitpoints;
		}
		public void setHitpoints(float hitpoints) {
			this.hitpoints = hitpoints;
		}
		
		public GameStateUnit move(Direction d) {
			GameStateUnit result = new GameStateUnit(this);
			result.xLoc+= d.xComponent();
			result.yLoc+= d.yComponent();
			return result;
		}
		
		public GameStateUnit takeDamage(float delta) {
			float armor = originalUnit.getTemplateView().getArmor(); 
			
			// predict armor effects
			if(delta<0) {
				if(-delta < armor) {
					delta = 0;
				}
				else {
					delta += armor;
				}
			}			
			GameStateUnit result = new GameStateUnit(this);
			result.hitpoints += delta;
			return result;
		}
		
		public void basicAttack(GameStateUnit target) {
			// compute distance
			if(DistanceMetrics.euclideanDistance(xLoc, yLoc, target.xLoc, target.yLoc) <= target.getOriginalUnit().getTemplateView().getRange()) {
				target.takeDamage(originalUnit.getTemplateView().getBasicAttack());
			}
		}
		
		public boolean isDead() {
			return hitpoints <= 0;
		}
	}
}
