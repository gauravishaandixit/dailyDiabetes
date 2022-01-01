package com.dailydiabetes.ui

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dailydiabetes.R
import com.dailydiabetes.model.DiabetesValue
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import org.achartengine.ChartFactory
import org.achartengine.GraphicalView
import org.achartengine.chart.PointStyle
import org.achartengine.model.XYMultipleSeriesDataset
import org.achartengine.model.XYSeries
import org.achartengine.renderer.XYMultipleSeriesRenderer
import org.achartengine.renderer.XYSeriesRenderer


class GraphActivity : AppCompatActivity() {
    private val TAG = "GraphActivity"

    private lateinit var xySeries: XYSeries
    private lateinit var xySeriesRenderer: XYSeriesRenderer
    private lateinit var xyMultipleSeriesRenderer: XYMultipleSeriesRenderer
    private lateinit var xyMultipleSeriesDataset: XYMultipleSeriesDataset
    private lateinit var chart: GraphicalView

    private lateinit var linearLayout: LinearLayout

    lateinit var dbReference: DatabaseReference

    lateinit var spinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)

        linearLayout = findViewById(R.id.graphView)
        dbReference = Firebase.database.getReference("diabetesValues")
        spinner = findViewById(R.id.chartSpinner)

        xySeries = XYSeries("Diabetes Value")

        xySeriesRenderer = XYSeriesRenderer()
        xySeriesRenderer.lineWidth = 5F
        xySeriesRenderer.color = Color.GREEN
        xySeriesRenderer.pointStyle = PointStyle.CIRCLE
        xySeriesRenderer.pointStrokeWidth = 5F

        xyMultipleSeriesRenderer = XYMultipleSeriesRenderer()
        xyMultipleSeriesRenderer.xAxisMin = -5.0
        xyMultipleSeriesRenderer.xAxisMax = 30.0
        xyMultipleSeriesRenderer.yAxisMin = 80.0
        xyMultipleSeriesRenderer.yAxisMax = 250.0
        xyMultipleSeriesRenderer.setYAxisAlign(Paint.Align.RIGHT, 0)
        xyMultipleSeriesRenderer.legendTextSize = 30F
        xyMultipleSeriesRenderer.labelsTextSize = 30F
        xyMultipleSeriesRenderer.setYLabelsColor(0, Color.RED)
        xyMultipleSeriesRenderer.xLabelsColor = Color.RED
        xyMultipleSeriesRenderer.isFitLegend = true
        xyMultipleSeriesRenderer.isShowGridX = true
        xyMultipleSeriesRenderer.isFitLegend = true
        xyMultipleSeriesRenderer.chartTitle = "Diabetes Value"
        xyMultipleSeriesRenderer.chartTitleTextSize = 50F
        xyMultipleSeriesRenderer.addSeriesRenderer(xySeriesRenderer)

        xyMultipleSeriesDataset = XYMultipleSeriesDataset()
        xyMultipleSeriesDataset.addSeries(xySeries)

        chart = ChartFactory.getLineChartView(
            this, xyMultipleSeriesDataset,
            xyMultipleSeriesRenderer
        )

        linearLayout.addView(chart)
        chart.invalidate()

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val selectedItem = p0?.getItemAtPosition(p2).toString()
                Log.i(TAG, "onItemSelected: $selectedItem")
                makeChart(selectedItem)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }

        chart.setOnClickListener {
            Log.i(TAG, "onChartClick: ")
            val seriesSelection = chart.currentSeriesAndPoint
            if(seriesSelection != null) {
                Log.i(TAG, "onCreate: $seriesSelection")
                Toast.makeText(
                    this,
                    "Chart element in series index " + seriesSelection.getSeriesIndex()
                            + " data point index " + seriesSelection.getPointIndex() + " was clicked"
                            + " closest point value X=" + seriesSelection.getXValue() + ", Y=" +
                            seriesSelection.getValue(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun makeChart(selectedItem: String = "Night") {
        Log.i(TAG, "makeChart: $selectedItem")
        var now = -1L
        dbReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                xySeries.clear()
                if(snapshot.exists()) {
                    for (diabetesValues in snapshot.children) {
                        val diabetesValue = diabetesValues.getValue(DiabetesValue::class.java)
                        if (diabetesValue != null && diabetesValue.time == selectedItem) {
                            if(now == -1L)
                                now = diabetesValue.date

                            val diff = (diabetesValue.date - now) / 86400000
                            xySeries.add(diff.toDouble(), diabetesValue.value.toDouble())
                        }
                    }
                    chart.invalidate()
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
}