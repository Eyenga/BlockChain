import java.util.List;
import java.util.ArrayList;

// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

public class BlockChain
{
	/**
	 * a node in the block-chain can have only one previous node, but may have any
	 * number of nodes >= 0 IMMDEDIATELY after it on the block-chain. The case where
	 * number of nextNode > 1 indicates a fork in the chain.
	 *
	 */
	private class BlockNode
	{
		protected Block block;
		protected BlockNode parentNode;
		protected List<BlockNode> childNodes;
		protected UTXOPool utxos;
		protected int blockNumber;
		protected int height;

		public BlockNode(Block blk, BlockNode parent)
		{
			block = blk;
			parentNode = parent;
			childNodes = new ArrayList<BlockNode>();
			utxos = new UTXOPool();

			if (parentNode != null)
			{
				blockNumber = parentNode.blockNumber + 1;
				height = parentNode.height + 1;
			} else
			{
				blockNumber = 0;
				height = 0;
			}
		}

		public BlockNode(Block blk, BlockNode parent, UTXOPool uPool)
		{
			block = blk;
			parentNode = parent;
			childNodes = new ArrayList<BlockNode>();
			utxos = uPool;

			if (parentNode != null)
			{
				blockNumber = parentNode.blockNumber + 1;
				height = parentNode.height + 1;
			} else
			{
				blockNumber = 0;
				height = 0;
			}
		}

		public BlockNode(Block blk, BlockNode parent, int blkNum, int blkHeight)
		{
			block = blk;
			parentNode = parent;
			childNodes = new ArrayList<BlockNode>();
			utxos = new UTXOPool();
			blockNumber = blkNum;
			height = blkHeight;
		}

		public BlockNode(Block blk, BlockNode parent, UTXOPool uPool, int blkNum, int blkHeight)
		{
			block = blk;
			parentNode = parent;
			childNodes = new ArrayList<BlockNode>();
			utxos = uPool;
			blockNumber = blkNum;
			height = blkHeight;
		}

		public BlockNode(BlockNode node)
		{
			block = node.block;
			parentNode = node.parentNode;
			childNodes = node.childNodes;
			utxos = node.utxos;
			blockNumber = node.blockNumber;
			height = node.height;
		}

	} // End of BlockNode class

	public static final int CUT_OFF_AGE = 10;

	BlockNode genesis;
	TransactionPool txPool;
	TxHandler txHandler;

	/**
	 * create an empty block chain with just a genesis block. Assume
	 * {@code genesisBlock} is a valid block
	 */
	public BlockChain(Block genesisBlock)
	{
		txPool = new TransactionPool();

		Transaction[] coinbase = { genesisBlock.getCoinbase() };
		Transaction[] tx = genesisBlock.getTransactions().toArray(new Transaction[0]);
		txHandler = new TxHandler(new UTXOPool());
		txHandler.handleTxs(coinbase);
		txHandler.handleTxs(tx);

		genesis = new BlockNode(genesisBlock, null, txHandler.getUTXOPool());

	}

	/** Get the maximum height block */
	public Block getMaxHeightBlock()
	{
		return getMaxHeightNode(genesis).block;
	}

	/**
	 * Returns the node at the greatest height of the chain. If multiple nodes are
	 * at the max height return the oldest, i.e the one who's block number is the
	 * lowest
	 * 
	 * @param head
	 *            The top node of current chain/sub-chain
	 * @return The oldest BlockNode at the top height.
	 */
	private BlockNode getMaxHeightNode(BlockNode head)
	{
		BlockNode blockToReturn = head;

		if (!head.childNodes.isEmpty())
		{

			BlockNode candidate;
			for (BlockNode child : head.childNodes)
			{
				candidate = getMaxHeightNode(child);
				if (candidate.height > blockToReturn.height)
				{
					blockToReturn = candidate;
				} else if (candidate.height == blockToReturn.height)
				{
					if (candidate.blockNumber < blockToReturn.blockNumber)
					{
						blockToReturn = candidate;
					}
				}
			}
		}

		return blockToReturn;
	}

	/** Get the UTXOPool for mining a new block on top of max height block */
	public UTXOPool getMaxHeightUTXOPool()
	{
		return txHandler.getUTXOPool();
	}

	/** Get the transaction pool to mine a new block */
	public TransactionPool getTransactionPool()
	{
		return txPool;
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
	public boolean addBlock(Block block)
	{
		if (block.getPrevBlockHash() == null) { return false; }

		BlockNode parent = findBlock(genesis, block.getPrevBlockHash());
		if (parent == null) { return false; }

		if (parent.height < (getMaxHeightNode(genesis).height - CUT_OFF_AGE)) { return false; }

		TxHandler handler = new TxHandler(parent.utxos);
		Transaction[] candidates = block.getTransactions().toArray(new Transaction[0]);
		if (handler.handleTxs(candidates).length != candidates.length) { return false; }

		BlockNode blockToAdd = new BlockNode(block, parent, handler.getUTXOPool());
		parent.childNodes.add(blockToAdd);

		return true;
	}

	/**
	 * Returns the node containing the block who's hash value is equal to
	 * {@code hash} or null if such a block does not exist in the block-chain
	 * 
	 * @param head
	 *            The top node of current chain/sub-chain
	 * @param hash
	 *            The hash of the block to find
	 * @return The BlockNode containing the particular block the was searched for or
	 *         null if no such block is in the block-chain
	 */
	private BlockNode findBlock(BlockNode head, byte[] hash)
	{
		BlockNode nodeToReturn = null;
		ByteArrayWrapper blockHash = new ByteArrayWrapper(hash);

		if (blockHash.equals(new ByteArrayWrapper(head.block.getHash())))
		{
			nodeToReturn = head;
		} else if (!head.childNodes.isEmpty())
		{
			for (BlockNode child : head.childNodes)
			{
				nodeToReturn = findBlock(child, hash);

				if (nodeToReturn != null)
				{
					break;
				}
			}
		}

		return nodeToReturn;
	}

	/** Add a transaction to the transaction pool */
	public void addTransaction(Transaction tx)
	{
		txPool.addTransaction(tx);
	}
}