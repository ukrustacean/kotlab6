import processing.core.PApplet
import processing.core.PVector

fun main() {
    PApplet.runSketch(arrayOf("Sketch"), Sketch)
}

class DisjointSets(private val n: Int) {
    val parents = IntArray(n) { i -> i }

    fun union(parent: Int, child: Int) { parents[child] = parent }
    fun findRoot(x: Int): Int {
        val arr = ArrayList<Int>(n)

        var i = x
        while (parents[i] != i) {
            arr.add(i)
            i = parents[i]
        }
        val parent = parents[i]

        for (j in arr) parents[j] = parent

        return parent
    }
}

object Sketch : PApplet() {
    data class Edge(val nodes: Pair<Int, Int>, val weight: Float)

    // Extensions
    operator fun PVector.plus(u: PVector): PVector = PVector.add(this, u)
    operator fun PVector.minus(u: PVector): PVector = PVector.sub(this, u)
    operator fun PVector.unaryMinus(): PVector = PVector(-x, -y, -z)
    private infix fun PVector.lineTo(u: PVector): Unit = line(x, y, u.x, u.y)

    // Constants
    private const val VARIANT = 3106
    private val n = intArrayOf(0, 3, 1, 0, 6)
    private val k = 1.0 - n[3] * 0.01 - n[4] * 0.005 - 0.05
    private val N = n[3] + 10

    // Don't forget to seed RNG!
    init {
        randomSeed(VARIANT.toLong())
    }

    // Graph data
    private val matrix = Array(N) { BooleanArray(N) { 1 <= random(2F) * k } }
    private val unimatrix = Array(N) { i -> BooleanArray(N) { j -> matrix[i][j] || matrix[j][i] } }
    private val B = Array(N) { FloatArray(N) { random(2F) } }
    private val C = Array(N) { i -> FloatArray(N) { j -> if (unimatrix[i][j]) ceil(B[i][j] * 100F).toFloat() else 0F } }
    private val D = Array(N) { i -> BooleanArray(N) { j -> C[i][j] > 0F } }
    private val H = Array(N) { i -> BooleanArray(N) { j -> D[i][j] != D[j][i] } }
    private val Tr = Array(N) { i -> BooleanArray(N) { j -> i < j } }
    private val W = run {
        val w = Array(N) { FloatArray(N) }

        for (i in 0..<N)
            for (j in i..<N) {
                val d =  if ( D[i][j]) 1F else 0F
                val h =  if ( H[i][j]) 1F else 0F
                val tr = if (Tr[i][j]) 1F else 0F
                val c = C[i][j]

                w[i][j] = (d + h * tr) * c
                w[j][i] = w[i][j]
            }

        return@run w
    }

    private val points: Array<PVector> =
        (1..<N).map { TWO_PI * it / (N - 1) }.map { PVector(cos(it) * 280, sin(it) * 280) }.toTypedArray() + PVector(
            0F, 0F
        )

    // Global state
    private var directed = false

    // Minimum spanning tree routines
    private val displayMatrix = Array(N) { BooleanArray(N) { false } }
    private val edges = mutableListOf<Edge>()
    private val sets = DisjointSets(N)

    private fun initKruskal() {
        for (i in displayMatrix.indices)
            for (j in displayMatrix[i].indices) {
                if (i < j && unimatrix[i][j]) edges.add(Edge(Pair(i, j), W[i][j]))
                displayMatrix[i][j] = false
            }

        edges.sortBy { it.weight }
    }

    private tailrec fun kruskalStep() {
        val root = sets.findRoot(0)
        if ((0..<N).all { sets.findRoot(it) == root }) return
        val e = edges.removeFirstOrNull() ?: return

        val a = e.nodes.first
        val b = e.nodes.second
        val aRoot = sets.findRoot(a)
        val bRoot = sets.findRoot(b)
        if (aRoot == bRoot) return kruskalStep()

        displayMatrix[a][b] = true
        var x = displayMatrix.mapIndexed { y, row -> row.mapIndexed { x, cell -> if (cell) W[y][x] else 0F } }.flatten().sum()
        println(x)
        sets.union(aRoot, bRoot)
    }

    // Drawing routines
    override fun settings(): Unit = size(700, 700)

    override fun setup() {
        directed = false
        println("Adjacency matrix:")
        println(unimatrix.joinToString("\n") { it.joinToString("  ") { x -> if (x) "1" else "0" } })
        println()

        println("Weight matrix:")
        println(W.joinToString("\n") { it.joinToString("  ") { x -> "%3d".format(x.toInt()) } })
        println()

        windowTitle("Circle Graph")

        colorMode(HSB, 360F, 100F, 100F)
        strokeWeight(2F)
        stroke(255)

        textAlign(CENTER, CENTER)
        textSize(30F)
    }

    override fun draw() {
        background(10)
        translate(width / 2F, height / 2F)

        for ((i, point) in points.withIndex()) {
            for ((j, edge) in unimatrix[i].withIndex()) {
                if (!edge || i >= j) continue

                push()
                run {
                    translate(point.x, point.y)

                    if(displayMatrix[i][j] || displayMatrix[j][i]) stroke(color(0, 100, 100))
                    val lineOffset = if (matrix[i][j] && matrix[j][i] && directed) 3F else 0F
                    val end = points[j] - point
                    val dir = -end
                    val offset = dir.copy()
                    offset.setMag(30F)

                    rotate(end.heading())
                    if (directed) arrow(PI, PVector(end.mag() - 30, lineOffset))
                    line(0F, lineOffset, end.mag(), lineOffset)
                }
                pop()
            }
        }

        for ((i, point) in points.withIndex()) {
            for ((j, edge) in unimatrix[i].withIndex()) {
                if (!edge || i >= j) continue

                push()
                run {
                    translate(point.x, point.y)

                    if(displayMatrix[i][j] || displayMatrix[j][i]) fill(color(0, 100, 100))
                    val end = points[j] - point
                    val dir = -end
                    val offset = dir.copy()
                    offset.setMag(30F)

                    rotate(end.heading())
                    textSize(20F)
                    translate(end.mag() / 2F - 33F, 0F)
                    rotate(-end.heading())

                    push()
                    fill(0F, 0F, 0F)
                    stroke(0F, 0F, 0F)
                    circle(0F, 0F, 20F)
                    pop()

                    textAlign(CENTER, CENTER)
                    text(W[i][j].toInt(), 0F, 0F)
                }
                pop()
            }
        }

        for ((i, point) in points.withIndex()) {
            push()
            run {
                translate(point.x, point.y)

                if (matrix[i][i]) {
                    noFill()
                    circle(0F, -40.83F, 40F)
                    if (directed) arrow(-1f, PVector(14f, -26.5f))
                }


                fill(100)
                circle(0F, 0F, 60F)
                fill(255)
                text((i + 1F).toInt(), 0F, 0F)
            }
            pop()
        }
    }

    private fun arrow(phi: Float, p: PVector) {
        p lineTo PVector(p.x + 15 * cos(phi + 0.3F), p.y + 15 * sin(phi + 0.3F))
        p lineTo PVector(p.x + 15 * cos(phi - 0.3F), p.y + 15 * sin(phi - 0.3F))
    }

    override fun keyPressed() {
        when (key) {
            ' ' -> directed = !directed
            'i' -> initKruskal()
            's' -> kruskalStep()
        }
    }
}
