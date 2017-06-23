package jp.ac.tsukuba.cs.mdl.numj.core;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class NdIndexer {

    private int[] pointers;

    private int dim;

    private int size;

    private int[] shape;

    private int[] stride;

    private int[] permutation;

    public static int computeSize(int[] shape) {
        return Arrays.stream(shape).reduce((l,r)->l*r).orElse(0);
    }

    public NdIndexer(int[] shape) {

        dim = shape.length;

        this.shape = shape;

        stride = createStride(shape);

        size = computeSize(shape);

        this.pointers = IntStream.range(0, size).toArray();

        permutation = IntStream.range(0, dim).toArray();
    }

    public NdIndexer(int[] shape, int[] pointers, int[] permutation) {

        dim = shape.length;

        this.shape = shape;

        this.stride = createStride(shape);

        this.permutation=permutation;

        size = pointers.length;

        this.pointers = pointers;
    }

    public NdIndexer transpose(int... permute){
        int[] newPermutation = new int[dim];
        for (int i=0;i<dim;i++){
            newPermutation[i] = permutation[permute[i]];
        }
        return new NdIndexer(shape, pointers, newPermutation);
    }

    public int[] getPermutation() {
        return permutation;
    }

    public NdIndexer reshape(int... shape){
        int[] newShape = new int[dim];
        for (int i=0;i<dim;i++){
            newShape[permutation[i]] = shape[i];
        }
        return new NdIndexer(newShape, pointers, permutation);
    }


    private static int[] createStride(int... shape) {
        int[] stride = new int[shape.length];
        stride[shape.length - 1] = 1;
        for (int i = shape.length - 2; i >= 0; i--) {
            stride[i] = stride[i + 1] * shape[i+1];
        }
        return stride;
    }

    public int pointer(int[] coordinate) {
        return pointers[
                IntStream
                        .range(0, dim)
                        .map(i -> coordinate[i] * stride[permutation[i]])
                        .sum()
                ];
    }

    public int pointerIndex(int pointer){
        return Arrays.binarySearch(pointers, pointer);
    }

    public int[] coordinate(int index) {
        int[] result = new int[dim];
        for (int i = 0; i < dim; i++) {
            result[i] = index / stride[i];
            index %= stride[i];
        }
        return result;
    }

    public int[] pointers() {
        return pointers;
    }

    public NdIndexer slice(NdIndex[] indices) {
        if (indices.length != dim) {
            throw new IllegalArgumentException();
        }

        int[] shape = new int[dim];
        List<List<Integer>> lst = Lists.newArrayList();
        for (int i = 0; i < dim; i++) {
            List<Integer> l = Lists.newArrayList();
            for (int j = 0; j < this.shape[permutation[i]]; j++) {
                indices[i].map(j).ifPresent(l::add);
            }
            shape[permutation[i]] = l.size();
            lst.add(l);
        }

        List<Integer> newPointer = Lists.newArrayList();
        for (List<Integer> coordinate : Lists.cartesianProduct(lst)) {
            newPointer.add(pointer(Ints.toArray(coordinate)));
        }

        return new NdIndexer(shape, Ints.toArray(newPointer), permutation);
    }

    public int getDim() {
        return dim;
    }


    public int getSize() {
        return size;
    }


    public int[] getShape() {
        int[] shapeView = new int[dim];
        for (int i=0;i<dim;i++){
            shapeView[i] = shape[permutation[i]];
        }
        return shapeView;
    }


    public int[] getStride() {
        return stride;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NdIndexer ndIndexer = (NdIndexer) o;

        if (dim != ndIndexer.dim) return false;
        if (size != ndIndexer.size) return false;
        if (!Arrays.equals(pointers, ndIndexer.pointers)) return false;
        if (!Arrays.equals(shape, ndIndexer.shape)) return false;
        return Arrays.equals(stride, ndIndexer.stride);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(pointers);
        result = 31 * result + dim;
        result = 31 * result + size;
        result = 31 * result + Arrays.hashCode(shape);
        result = 31 * result + Arrays.hashCode(stride);
        return result;
    }
}
