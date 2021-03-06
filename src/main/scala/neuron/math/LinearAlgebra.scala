package neuron.math

/*******************************************************************************************/
// Numerical classes and their operations : interface to breeze
import breeze.generic._
import breeze.linalg._
import breeze.numerics._
import breeze.optimize._
import breeze.stats.distributions._
import breeze.util._

class LabelVector(val data:DenseVector[Int]) {
  def length = data.length
  def concatenate (that: LabelVector) = new LabelVector(DenseVector.vertcat(this.data, that.data))
  def splice(num: Int) : (LabelVector, LabelVector) = (new LabelVector(this.data(0 until num)), new LabelVector(this.data(num to -1)))
}
class NeuronVector (val data: DenseVector[Double]) {
  def length = data.length
  def this(n:Int) = this(DenseVector.zeros[Double] (n))
  def this(n:Int, rand: Rand[Double]) = this(DenseVector.rand(n, rand)) // uniform sampling, might not be a good default choice
  def this(n:Int, rand: => Rand[Boolean]) = this(I(DenseVector.rand(n, rand)))
  def this(arr: Array[Double], offset: Int) = this(new DenseVector(arr, offset))
  def this(arr: Array[Double]) = this(new DenseVector(arr))
  def this(arr: Array[Double], offset: Int, stride: Int, length: Int) = this(new DenseVector(arr, offset, stride, length))
  def apply(n: Int): Double = data(n) 
  def update(n:Int, e:Double): Unit = data.update(n,e)
  def apply(r: Range): NeuronVector = new NeuronVector(data(r))
  def update(r: Range, v:NeuronVector): Unit = {data(r):=v.data}
  def apply(idx: IndexedSeq[Int]): NeuronVector = new NeuronVector(data(idx).toDenseVector)
  def update(r: IndexedSeq[Int], v:NeuronVector): Unit = {data(r):=v.data}
  def concatenate (that: NeuronVector) : NeuronVector = new NeuronVector(DenseVector.vertcat(this.data, that.data))
  def splice(num: Int) : (NeuronVector, NeuronVector) = (new NeuronVector(this.data(0 until num)), new NeuronVector(this.data(num to -1)))

  def -(that:NeuronVector): NeuronVector = new NeuronVector(this.data - that.data)
  def +(that:NeuronVector): NeuronVector = new NeuronVector(this.data + that.data)
  def +(x:Double) : NeuronVector = new NeuronVector(this.data + x)
  def -(x:Double) : NeuronVector = new NeuronVector(this.data - x)
  def *(x:Double) : NeuronVector = new NeuronVector(this.data * x)
  def /(x:Double) : NeuronVector = new NeuronVector(this.data / x)
  def :=(that: NeuronVector): NeuronVector = {this.data := that.data; this}
  def +=(that: NeuronVector): NeuronVector = {this.data :+= that.data; this }
  def :+=(that: NeuronVector): NeuronVector = {this.data :+= that.data; this }
  def -=(that: NeuronVector): NeuronVector = {this.data :-= that.data; this }
  def :-=(that: NeuronVector): NeuronVector = {this.data :-= that.data; this }
  def :+=(x:Double): NeuronVector = {this.data :+= x; this}
  def :-=(x:Double): NeuronVector = {this.data :-= x; this}
  def :*=(x:Double): NeuronVector = {this.data :*= x; this}
  def :/=(x:Double): NeuronVector = {this.data :/= x; this}
  def euclideanSqrNorm = {val z = norm(data); z*z}
  def :*(that: NeuronVector): NeuronVector = new NeuronVector(this.data :* that.data)
  def *=(that: NeuronVector): NeuronVector = {this.data :*= that.data; this}
  def :*=(that: NeuronVector): NeuronVector = {this.data :*= that.data; this}
  def CROSS (that: NeuronVector): NeuronMatrix = new NeuronMatrix(this.data.asDenseMatrix.t * that.data.asDenseMatrix)
  def dot(that: NeuronVector): Double = this.data dot that.data
  def := (x:Double) : NeuronVector = {data:=x;this}
  def copy(): NeuronVector = new NeuronVector(data.copy)
  def sum(): Double = breeze.linalg.sum(data)
  def min(): Double = breeze.linalg.min(data)
  def max(): Double = breeze.linalg.max(data)
  def argmax(): Int = breeze.linalg.argmax(data)
  def asNeuronMatrix(rows:Int, cols:Int): NeuronMatrix = new NeuronMatrix (data.asDenseMatrix.reshape(rows, cols)) 
  def last(): Double = data(data.length-1)
  def append(last: Double): NeuronVector = new NeuronVector(DenseVector.vertcat(data, DenseVector(last)) )
  def normalized(): NeuronVector = new NeuronVector(data/norm(data))
  override def toString() = data.data.mkString("\t") // has bug with colVec
  def toWeightVector(): WeightVector = new WeightVector(data)
  def importFromFile(filename: String): Unit = {
    val source = scala.io.Source.fromFile(filename)
    val dataBlock = source.mkString.split("\\s+").map(_.toDouble)
    source.close()
    assert(dataBlock.length == length)
    data := new DenseVector(dataBlock)
  }
  def filterMap(f: Double => Boolean) = new NeuronVector( data.map(x => if (f(x)) 1.0 else 0.0 ))
  def shuffle(): NeuronVector = {
    val buf = new NeuronVector(length)
    val shuffedList = scala.util.Random.shuffle((0 until length).toList)
    for (j<- 0 until length) {
	      buf.data(j) = data(shuffedList(j))
	}
    buf
  }   
  def argtopk(k: Int): NeuronVector = new NeuronVector({
    val topk = breeze.linalg.argtopk(this.data, k) 
    val indicator = DenseVector.zeros[Double] (length)
    indicator(topk) := 1.0
    indicator
  })
  def flr() = new NeuronVector(floor(data))
  def upperBoundTo(b:Double, roundoff:Double = 1E-10): Unit = {data(data :>= b) := b - roundoff}
  def lowerBoundTo(b:Double, roundoff:Double = 1E-10): Unit = {data(data :<= b) := b + roundoff}
  def boundTo(l:Double, u:Double, roundoff:Double = 1E-10): Unit = {
    lowerBoundTo(l, roundoff)
    upperBoundTo(u, roundoff)
  }
  
  def draw(): Int = {
    if (max() < 1E-9) // when no mass
       -1
    else
       new Multinomial(data).draw()
  }
  def sample(n: Int): IndexedSeq[Int] = {
    new Multinomial(data).sample(n)
  }
  def checkDiff(that: NeuronVector, tolerant: Double = 1E-6): Boolean = {
    all(((data - that.data) :< tolerant) :| 
    ((data - that.data) :< (abs(data) * tolerant)))
  }
  def binarized() = {
    val b = new NeuronVector(length)
    for(i<-0 until length) b.data(i) = I(new Bernoulli(data(i)).draw())
    b
  }
  def select(that: NeuronVector, check: NeuronVector): NeuronVector = {
    for(i<- 0 until length) 
      if (check.data(i) < 0) data(i) = that.data(i)
    this
  }
}
class NeuronMatrix (val data:DenseMatrix[Double]){
  def rows = data.rows
  def cols = data.cols
  def this(rows:Int, cols:Int) = this(DenseMatrix.zeros[Double](rows,cols))
  def this(rows:Int, cols:Int, rand: Rand[Double]) = this(DenseMatrix.rand(rows, cols, rand)) // will be fixed in next release
  def this(rows:Int, cols:Int, rand: => Rand[Boolean]) = this(I(DenseMatrix.rand(rows, cols, rand)))
  def this(rows:Int, arr: Array[Double]) = this(new DenseMatrix(rows, arr, 0))
  def this(rows:Int, cols:Int, arr: Array[Double]) = this(new DenseMatrix(rows, cols, arr, 0))
  def this(rows:Int, cols:Int, arr: Array[Double], offset: Int, majorStride: Int) = 
    this(new DenseMatrix(rows, cols, arr, offset, majorStride))
  def apply(row:Int, col:Int) = data(row,col)
  def update(row:Int, col:Int, e:Double) = data.update(row,col,e)
  def update(rows: Range, cols: Range, e: NeuronMatrix) = {data(rows, cols) := e.data}
  def diagonal(): NeuronVector = {new NeuronVector(diag(data))}
  def +(that: NeuronMatrix): NeuronMatrix = new NeuronMatrix(this.data + that.data)
  def -(that: NeuronMatrix): NeuronMatrix = new NeuronMatrix(this.data - that.data)  
  def Add(that: NeuronVector): NeuronMatrix = new NeuronMatrix(this.data(::, breeze.linalg.*) + that.data)
  def AddTrans(that:NeuronVector): NeuronMatrix = new NeuronMatrix(this.data(breeze.linalg.*, ::) + that.data)
  def AddWith(that: NeuronVector): NeuronMatrix = {this.data(::, breeze.linalg.*) += that.data; this}
  def AddTransWith(that:NeuronVector): NeuronMatrix = {this.data(breeze.linalg.*, ::) += that.data; this}
  def Minus(that: NeuronVector): NeuronMatrix = new NeuronMatrix(this.data(::, breeze.linalg.*) - that.data)
  def MinusTrans(that:NeuronVector): NeuronMatrix = new NeuronMatrix(this.data(breeze.linalg.*, ::) - that.data)
  def MultElem(that: NeuronVector): NeuronMatrix = new NeuronMatrix(this.data(::, breeze.linalg.*) :* that.data)
  def MultElemTrans(that:NeuronVector): NeuronMatrix = new NeuronMatrix(this.data(breeze.linalg.*, ::) :* that.data)
  def MultElemWith(that: NeuronVector): NeuronMatrix = {this.data(::, breeze.linalg.*) :*= that.data; this}
  def MultElemTransWith(that:NeuronVector): NeuronMatrix = {this.data(breeze.linalg.*, ::) :*= that.data; this}
  def DivElem(that: NeuronVector): NeuronMatrix = new NeuronMatrix(this.data(::, breeze.linalg.*) :/ that.data)
  def DivElemTrans(that:NeuronVector): NeuronMatrix = new NeuronMatrix(this.data(breeze.linalg.*, ::) :/ that.data)
  def Mult(x:NeuronVector) = new NeuronVector(data * x.data) //this * x
  def *(x:NeuronVector) = Mult(x)
  def TransMult(x:NeuronVector): NeuronVector = new NeuronVector(this.data.t * x.data)
  def Mult(x:NeuronMatrix): NeuronMatrix = new NeuronMatrix(this.data * x.data)
  def *(x:NeuronMatrix): NeuronMatrix = Mult(x)
  def TransMult(x:NeuronMatrix) = new NeuronMatrix(this.data.t * x.data)
  def MultTrans(x:NeuronMatrix) = new NeuronMatrix(this.data * x.data.t) 
  def \(x: NeuronVector) = new NeuronVector( this.data \ x.data)
  def \(x: NeuronMatrix) = new NeuronMatrix( this.data \ x.data)
  def *(x:Double): NeuronMatrix = new NeuronMatrix(this.data * x)
  def +(x:Double): NeuronMatrix = new NeuronMatrix(this.data + x)
  def -(x:Double): NeuronMatrix = new NeuronMatrix(this.data - x)
  def /(x:Double) : NeuronMatrix = new NeuronMatrix(this.data / x)
  def :=(x: Double) : NeuronMatrix={data:=x; this}
  def :=(that:NeuronMatrix): NeuronMatrix = {this.data := that.data; this}
  def +=(that:NeuronMatrix): NeuronMatrix = {this.data :+= that.data; this}
  def :+=(that:NeuronMatrix): NeuronMatrix = {this.data :+= that.data; this}
  def -=(that:NeuronMatrix): NeuronMatrix = {this.data :-= that.data; this}
  def :-=(that:NeuronMatrix): NeuronMatrix = {this.data :-= that.data; this}
  def :+=(x:Double): NeuronMatrix = {this.data :+= x; this}
  def :-=(x:Double): NeuronMatrix = {this.data :-= x; this}  
  def :*=(x:Double): NeuronMatrix = {this.data :*= x; this}
  def :/=(x:Double): NeuronMatrix = {this.data :/= x; this}
  def :*(that: NeuronMatrix): NeuronMatrix = new NeuronMatrix(this.data :* that.data)
  def :*=(that: NeuronMatrix): NeuronMatrix = {this.data :*= that.data; this}
  def reshape(r: Int, c: Int, isView: Boolean = true) = new NeuronMatrix(data.reshape(r,c, isView))
  def vec(isView: Boolean = true) = new NeuronVector(data.flatten(isView))  // important!
  def transpose = new NeuronMatrix(data.t)
  def euclideanSqrNorm: Double = {sum(data :* data)}
  def euclideanSqrNormCol: NeuronVector = {val z = data:*data; new NeuronVector(sum(z(::,breeze.linalg.*)).toDenseVector)}
  def euclideanSqrNormRow: NeuronVector = {val z = data:*data; new NeuronVector(sum(z(breeze.linalg.*, ::)).toDenseVector)}
  def sumCol() = new NeuronVector(sum(data(::,breeze.linalg.*)).toDenseVector)
  def sumRow() = new NeuronVector(sum(data(breeze.linalg.*,::)).toDenseVector)
  def sumAll():Double = sum(data)
  def maxCol() = new NeuronVector(max(data(::,breeze.linalg.*)).toDenseVector)
  def maxAll(): Double = max(data)
  def argmaxCol() = new LabelVector(argmax(data(::,breeze.linalg.*)).toDenseVector)
  def colVec(i: Int) = new NeuronVector(data(::,i))
  def rowVec(i: Int) = new NeuronVector(data(i,::).t)
  def copy() = new NeuronMatrix(data.copy)
  def spliceRow(num: Int): (NeuronMatrix, NeuronMatrix) = (new NeuronMatrix(this.data(0 until num, ::)), new NeuronMatrix(this.data(num to -1, ::)))
  def padRow(that: NeuronMatrix) = new NeuronMatrix(DenseMatrix.vertcat(this.data, that.data))
  def Cols(range: Range) = new NeuronMatrix(data(::,range))
  def Rows(range: Range) = new NeuronMatrix(data(range, ::))
  def Cols(idx: IndexedSeq[Int]) = new NeuronMatrix(data(::, idx).toDenseMatrix)
  def Rows(idx: IndexedSeq[Int]) = new NeuronMatrix(data(idx, ::).toDenseMatrix)
  def CROSS (that: NeuronMatrix): NeuronTensor = {
    assert(this.cols == that.cols)
    val m = new NeuronMatrix(this.rows * that.rows, this.cols)
    for (i<- 0 until that.rows) {// vercat that this.rows times
      m.Rows(i*this.rows until (i+1)*this.rows) := new NeuronMatrix(this.data(breeze.linalg.*, ::) :* that.data(i, ::).t)
    }
    new NeuronTensor(m.data, this.rows, that.rows)
  }
  def asNeuronTensor(rows:Int, cols:Int): NeuronTensor = new NeuronTensor(data, rows, cols)
  def importFromFile(filename: String): Unit = {
    val source = scala.io.Source.fromFile(filename)
    val dataBlock = source.mkString.split("\\s+").map(_.toDouble)
    source.close()
    assert(dataBlock.length == rows*cols)
    data := new DenseMatrix(rows, cols, dataBlock)
  }
  def filterMap(f: Double => Boolean) = new NeuronMatrix( data.map(x => if (f(x)) 1.0 else 0.0 ))
  def shuffleCols(): NeuronMatrix = {
    val buf = new NeuronMatrix(rows, cols)
    val shuffedList = scala.util.Random.shuffle((0 until cols).toList)
    for (j<- 0 until cols) {
	      buf.colVec(j) := colVec(shuffedList(j))
	}
    buf
  }
  def shuffleRows(): NeuronMatrix = {
    val buf = new NeuronMatrix(rows, cols)
    val shuffedList = scala.util.Random.shuffle((0 until cols).toList)
    for (j<- 0 until cols) {
	      buf.rowVec(j) := rowVec(shuffedList(j))
	}
    buf
  }  
  def argtopk(k: Int): NeuronMatrix = {
    val indicator = new NeuronMatrix(rows, cols)
    for (i<- 0 until cols) {
      val topk = breeze.linalg.argtopk(this.data(::, i), k) //TopK[Int, Double](k, 0 until rows, j=>this.data(j, i)).toSeq
      indicator.data(topk.map((_, i))) := 1.0
    }
    indicator
  }
  def flr() = new NeuronMatrix(floor(data))
  def upperBoundTo(b:Double, roundoff:Double = 1E-10): Unit = {data(data :>= b) := b - roundoff}
  def lowerBoundTo(b:Double, roundoff:Double = 1E-10): Unit = {data(data :<= b) := b + roundoff}
  def boundTo(l:Double, u:Double, roundoff:Double = 1E-10): Unit = {
    lowerBoundTo(l, roundoff)
    upperBoundTo(u, roundoff)
  }  
  def drawByCols(idx: IndexedSeq[Int] = 0 until cols): IndexedSeq[Int] = {
    idx.map(colVec(_).draw())
  }
  def sampleByCols(idx: IndexedSeq[Int] = 0 until cols, n:Int = 1): IndexedSeq[Int] = {
    idx.flatMap(colVec(_).sample(n))
  }
  def drawByRows(idx: IndexedSeq[Int] = 0 until rows): IndexedSeq[Int] = {
    idx.map(rowVec(_).draw())
  }
  def sampleByRows(idx: IndexedSeq[Int] = 0 until cols, n:Int = 1): IndexedSeq[Int] = {
    idx.flatMap(rowVec(_).sample(n))
  }
  def binarized(isRandom: Boolean = true, threshold: Double = 0.5) = {
    val b = new NeuronMatrix(rows, cols)
    if (isRandom) 
      for(i<-0 until rows; j<-0 until cols) b.data(i,j) = I(new Bernoulli(data(i,j)).draw())
    else
      b.data := I(data :> threshold)
    b
  }
  def select(that: NeuronMatrix, check: NeuronVector): NeuronMatrix = {
    for(i<- 0 until cols) 
      if (check.data(i) < 0) {
        data(::, i) :=  that.data(::, i)
      }
    this
  }
  
  def writeMat(filename: String, dataname: String): Unit = {
    import com.jmatio.io._
    import com.jmatio.types._
    import java.util.ArrayList
    
    val list = new ArrayList[MLArray]()
    list.add(new MLDouble(dataname, data.flatten(false).data, data.rows))
    new MatFileWriter(filename, list)    
  }
}

// solution to 3-order tensor
class NeuronTensor(val data: DenseMatrix[Double], val d1: Int, val d2: Int) {
  assert(d1*d2 == data.rows)
  val d3 = data.cols
  def this(d1: Int, d2: Int, d3: Int) = this(DenseMatrix.zeros[Double](d1*d2,d3), d1, d2)
  def this(d1: Int, d2: Int, d3: Int, rand: Rand[Double]) = this(DenseMatrix.rand(d1*d2,d3,rand), d1, d2)
  def mat(isView: Boolean = true) = new NeuronMatrix({if (isView) data; else data.copy})
  def Mult(that: NeuronMatrix): NeuronMatrix = {// matrix-vector mult with batch
    assert(d2 == that.rows && d3 == that.cols)
    val m = new NeuronMatrix(d1, d3) 
    for (i<- 0 until d3) {
      m.data(::, i) := data(::,i).asDenseMatrix.reshape(d1, d2) * that.data(::,i)
    }
    m
  }
  def TransMult(that: NeuronMatrix): NeuronMatrix = {// matrix-vector mult with batch
    assert(d1 == that.rows && d3 == that.cols)
    val m = new NeuronMatrix(d1, d3) 
    for (i<- 0 until d3) {
      m.data(::, i) := data(::,i).asDenseMatrix.reshape(d1, d2).t * that.data(::,i)
    }
    m
  }
  def MultLeft(left: NeuronMatrix): NeuronTensor = {// (id)matrix-matrix mult with batch
    assert(left.cols == d1)
    new NeuronTensor((left.data * data.reshape(d1, d2*d3)).reshape(left.rows * d2, d3), left.rows, d2)
    }
  def MultRight(right: NeuronMatrix): NeuronTensor = {// matrix-(id)matrix mult with batch
    assert(right.rows == d2)
    val m = new NeuronTensor(d1, right.cols, d3)
	  for (i<- 0 until d3) {
	    m.data(::, i) := (data(::, i).asDenseMatrix.reshape(d1, d2) * right.data).flatten()
	  }
     m
    }
  def MultLeftAndRight(left: NeuronMatrix, right:NeuronMatrix) = {
    this.MultLeft(left).MultRight(right)
    }
} 


class WeightVector (data: DenseVector[Double]) extends NeuronVector(data) {
  def this(n:Int) = this(DenseVector.zeros[Double](n))
  def this(n:Int, rand: Rand[Double]) = this(DenseVector.rand(n, rand))
  def concatenate (that: WeightVector) : WeightVector = new WeightVector(DenseVector.vertcat(this.data, that.data))
  var ptr : Int = 0
  def reset(): Unit = {ptr = 0; }
  def apply(W:NeuronMatrix, b:NeuronVector): Int = {
    if (W != null) {
    val rows = W.data.rows
    val cols = W.data.cols
    
    W.data := data(ptr until ptr + rows*cols).asDenseMatrix.reshape(rows, cols)
    ptr = (ptr + rows * cols) % length
    }
    if (b != null) {
    b.data := data(ptr until ptr + b.length)
    ptr = (ptr + b.length) % length
    }
    ptr
  }
  def get(W:NeuronMatrix, b:NeuronVector): Int = {
    if (W != null) {
    val rows = W.data.rows
    val cols = W.data.cols
    
    data(ptr until ptr + rows*cols).asDenseMatrix.reshape(rows, cols) := W.data
    ptr = (ptr + rows * cols) % length
    }
    if (b != null) {
    data(ptr until ptr + b.length) := b.data
    ptr = (ptr + b.length) % length
    }
    ptr
  }
  def set(wv: NeuronVector): Int = {
    ptr = 0
    data := wv.data
    0
  }
  override def copy(): WeightVector = new WeightVector(data.copy)
}


object NullVector extends NeuronVector (0)
class OnesVector(n:Int) extends NeuronVector(DenseVector.ones[Double](n)) 

object NullMatrix extends NeuronMatrix (0,0)
class OnesMatrix(r:Int, c:Int) extends NeuronMatrix(DenseMatrix.ones[Double](r,c))


