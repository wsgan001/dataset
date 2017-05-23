package dataset.helper;

import java.io.File;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

import edu.stanford.nlp.util.ArrayUtils;
import structure.matrix.MatrixEntry;
import structure.matrix.SparseMatrix;
import yifan.utils.FileIO;
import static yifan.utils.IOUtils.*;

public class Dataset {

	private SparseMatrix rating;
	private SparseMatrix feature;
	private String dir;

	public static void main(String[] args) {
		try {
			Dataset dataset = new Dataset("/home/yifan/dataset/nips/select");
			dataset.selectFeature("/home/yifan/dataset/nips/select/select");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Dataset(String dir) throws IOException {
		this.dir = dir;
		rating = SparseMatrix.readMatrix(dir + "/rating");
		feature = SparseMatrix.readMatrix(dir + "/feature");
	}

	private SparseMatrix[] leaveOneOut() throws Exception {
		int[] row_ptr = rating.getRowPointers();
		int[] col_idx = rating.getColumnIndices();
		Random random = new Random(System.currentTimeMillis());
		Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
		Multimap<Integer, Integer> colMap = HashMultimap.create();
		for (int u = 0, um = rating.numRows(); u < um; u++) {
			int start = row_ptr[u], end = row_ptr[u + 1];
			int len = end - start;
			if (len <= 1)
				continue;
			int idx = random.nextInt(len) + start;
			int j = col_idx[idx];
			rating.get(u, j);
			dataTable.put(u, j, 1.0);
			colMap.put(j, u);
		}
		SparseMatrix test = new SparseMatrix(rating.numRows, rating.numColumns, dataTable, colMap);
		SparseMatrix train = getTrainFile(test);
		return new SparseMatrix[] { train, test };
	}

	public void leaveOneOut(int nfold) throws Exception {
		// TODO Auto-generated method stub
		// writeMatrix(feedback, "result");
		File LOO = new File(dir + "/LOO");
		if (!LOO.isDirectory())
			LOO.mkdir();
		for (int f = 1; f <= nfold; f++) {
			SparseMatrix[] datafold = leaveOneOut();
			datafold[0].writeMatrix(dir + "/LOO/train" + f);
			datafold[1].writeMatrix(dir + "/LOO/test" + f);
		}
	}

	public SparseMatrix getTrainFile(SparseMatrix test) {
		SparseMatrix trainMatrix = new SparseMatrix(rating);
		for (MatrixEntry entry : test) {
			int u = entry.row();
			int i = entry.column();
			trainMatrix.set(u, i, 0.0);
		}
		SparseMatrix.reshape(trainMatrix);
		return trainMatrix;
	}

	public void selectFeature(String selectfile) throws Exception {
		String line = FileIO.readAsString(selectfile);
		String[] terms = line.split(" ");
		List<Integer> list = Lists.newArrayList();
		for (int i = 0; i < terms.length; i++) {
			double v = Double.parseDouble(terms[i]);
			if (v > 0)
				list.add(i);
		}
//		console(list);
		Integer[] select = new Integer[list.size()];
		list.toArray(select);

		SparseMatrix sub = feature.selectColumn(ArrayUtils.toPrimitive(select));
		sub.writeMatrix(dir + "/subfeature");
	}

}