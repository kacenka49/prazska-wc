package cz.bezvadilna.prazskawc.screen

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import cz.bezvadilna.prazskawc.R
import cz.bezvadilna.prazskawc.adapter.PointListAdapter
import cz.bezvadilna.prazskawc.model.Point
import kotlinx.android.synthetic.main.fragment_list.*

class ListFragment : Fragment(), PointViewerInterface {

    private lateinit var fragmentContext: Context
    //private lateinit var adapter: PointListAdapter
    //private var listener: OnPlaceListInteractionListener? = null
    //private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var holderActivity: PointHolderInterface

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        context?.let {
            fragmentContext = it
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //recyclerview
        //linearLayoutManager = LinearLayoutManager(context)
        //pointsListView.layoutManager = linearLayoutManager
        //
        //adapter = PointListAdapter(pointsList)
        //pointsListView.adapter = adapter
        //listPoints.adapter = pointListAdapter

        //For communicating with mainActivity
        holderActivity = activity as PointHolderInterface
        fetchPoints()
    }

    /*
    * Gets Points form holderActivity and sort them by distance
    */
    private fun fetchPoints(){
        //get data
        var rawData = holderActivity.cachedPoints
        //Sort by distance
        rawData.sortBy { it.distance }

        showPoints(rawData)
    }

    /**
     * Shows list of Points on pointsListView
     */
    private fun showPoints(points: List<Point>){
        pointsListView.layoutManager = LinearLayoutManager(fragmentContext)
        pointsListView.adapter = PointListAdapter(points, fragmentContext)
    }

    override fun onUpdateCachedPoints() {
        //TODO("Not yet implemented")
    }
}
