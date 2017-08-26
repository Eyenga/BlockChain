import java.util.ArrayList;

public class TxHandler {

	/**
	 * Set of current UTXOs to be processed
	 */
	private UTXOPool ledger;

	/**
	 * Creates a public ledger whose current UTXOPool (collection of unspent
	 * transaction outputs) is {@code utxoPool}. This should make a copy of utxoPool
	 * by using the UTXOPool(UTXOPool uPool) constructor.
	 */
	public TxHandler(UTXOPool utxoPool) {
		ledger = new UTXOPool(utxoPool);
	}

	/** @formatter:off
	 * @return true if: 
	 * 	(1) all outputs claimed by {@code tx} are in the currentUTXO pool, 
	 *  (2) the signatures on each input of {@code tx} are valid,
	 *  (3) no UTXO is claimed multiple times by {@code tx}, 
	 *  (4) all of {@code tx}s output values are non-negative, and 
	 *  (5) the sum of {@code tx}s input values is greater than or equal to the sum of its
	 *      output values; and false otherwise.
	 * @formatter:on
	 */
	public boolean isValidTx(Transaction tx)
	{

		/*
		 * It should be noted that the inputs of the current transaction (tx) are
		 * outputs of other, previous, transactions. Those outputs are stored in the
		 * UTXO pool and should not be confused with the outputs of the THIS transaction
		 * (tx)
		 */

		double sumOfInputs = 0, sumOfOutputs = 0;
		int[] outputsFound = new int[tx.numInputs()];
		for (int i = 0; i < tx.numInputs(); i++)
		{

			UTXO outputClaimed = new UTXO(tx.getInput(i).prevTxHash, tx.getInput(i).outputIndex);

			// (1) Verify inputs of transaction are in UTXO pool
			if (!ledger.contains(outputClaimed)) { return false; }

			// (2) verify signature of transactions inputs
			if (!Crypto.verifySignature(ledger.getTxOutput(outputClaimed).address, tx.getRawDataToSign(i),
					tx.getInput(i).signature)) { return false; }

			// (3) Verify no input is claimed more than once, i.e no double-spend
			outputsFound[i] = outputClaimed.hashCode();

			for (int j = 0; j < i; j++)
			{
				if (outputClaimed.hashCode() == outputsFound[j]) { return false; }
			}

			sumOfInputs += ledger.getTxOutput(outputClaimed).value;
		}

		// (4) Verify outputs of transaction have non-negative values
		for (int i = 0; i < tx.numOutputs(); i++)
		{
			if (tx.getOutput(i).value < 0) { return false; }

			sumOfOutputs += tx.getOutput(i).value;
		}

		// (5) Verify that sum of inputs >= sum of outputs
		if (sumOfInputs < sumOfOutputs) { return false; }

		return true;
	}

	/**
	 * Handles each epoch by receiving an unordered array of proposed transactions,
	 * checking each transaction for correctness, returning a mutually valid array
	 * of accepted transactions, and updating the current UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs)
	{
		ArrayList<Transaction> validTxs = new ArrayList<Transaction>();
		ArrayList<Transaction> inValidTxs = new ArrayList<Transaction>();
		Transaction[] acceptedTxs = new Transaction[validTxs.size()];

		for (Transaction tx : possibleTxs)
		{
			/*
			 * If a transaction is valid, update the UTXO pool and add it to list of valid
			 * transactions
			 */
			if (isValidTx(tx))
			{
				updateLedger(tx);
				validTxs.add(tx);
			} else
			{
				inValidTxs.add(tx);
			}
		}

		// Check if list of valid transaction is maximal size
		if ((validTxs.size() == possibleTxs.length) || validTxs.isEmpty()) { return validTxs.toArray(acceptedTxs); }

		int numOfInvalidTxs;
		do
		{
			numOfInvalidTxs = inValidTxs.size();

			// Iterate thought invalid transaction and check for any newly valid
			// transactions.
			for (int i = 0; i < inValidTxs.size(); i++)
			{
				Transaction tx = inValidTxs.get(i);

				if (isValidTx(tx))
				{
					updateLedger(tx);
					validTxs.add(tx);
					inValidTxs.remove(tx);
				}
			}

			// Continue loop until list of invalid transactions ceases to shrink.
		} while (inValidTxs.size() < numOfInvalidTxs);

		return validTxs.toArray(acceptedTxs);
	}

	/**
	 * Updates the current UTXO pool by removing the inputs claimed by {@code tx}
	 * and adding the outputs made by {@code tx}.
	 * 
	 * @param tx
	 */
	private void updateLedger(Transaction tx)
	{
		// Remove spent transaction inputs.
		for (Transaction.Input input : tx.getInputs())
		{
			UTXO stxi = new UTXO(input.prevTxHash, input.outputIndex);
			ledger.removeUTXO(stxi);
		}

		// Add unspent transaction outputs
		int i = 0;
		for (Transaction.Output output : tx.getOutputs())
		{
			UTXO utxo = new UTXO(tx.getHash(), i++);
			ledger.addUTXO(utxo, output);
		}
	}

}
