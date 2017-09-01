import java.util.List;
import java.util.ArrayList;
// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

public class BlockChain
{

	// a node in the block-chain can have only one previous node, but may have any
	// number of nodes >= 0 IMMDEDIATELY after it on the block-chain. The case where
	// number of nextNode > 1 indicates a fork in the chain.
	protected class BlockNode
	{
		protected Block block;
		protected int height;
		protected UTXOPool utxos;
		protected BlockNode previousNode;
		protected List<BlockNode> nextNode;

		public BlockNode(Block block) {

		}

	}

	public static final int CUT_OFF_AGE = 10;

	/**
	 * create an empty block chain with just a genesis block. Assume
	 * {@code genesisBlock} is a valid block
	 */
	public BlockChain(Block genesisBlock) {
		// IMPLEMENT THIS
	}

	/** Get the maximum height block */
	public Block getMaxHeightBlock() {
		// IMPLEMENT THIS
	}

	/** Get the UTXOPool for mining a new block on top of max height block */
	public UTXOPool getMaxHeightUTXOPool() {
		// IMPLEMENT THIS
	}

	/** Get the transaction pool to mine a new block */
	public TransactionPool getTransactionPool() {
		// IMPLEMENT THIS
	}

	/**
	 * Add {@code block} to the block chain if it is valid. For validity, all
	 * transactions should be valid and block should be at
	 * {@code height > (maxHeight - CUT_OFF_AGE)}.
	 * 
	 * <p>
	 * For example, you can try creating a new block over the genesis block (block
	 * height 2) if the block chain height is {@code <=
	 * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot
	 * create a new block at height 2.
	 * 
	 * @return true if block is successfully added
	 */
	public boolean addBlock(Block block) {
		// IMPLEMENT THIS
	}

	/** Add a transaction to the transaction pool */
	public void addTransaction(Transaction tx) {
		// IMPLEMENT THIS
	}
}