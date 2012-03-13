(
func:{ |eventSource, n, select|
	var nothing = EventSource();
    (0..n).collect{ |i|
    	select.flatCollect{ |v|
    		if( v == i ) {
    			eventSource
    		} {
    			nothing
    		}
    	}
    }
},
desc: "Returns a List of event streams with n elements where the events will be sent according to the values of of select. n: number of event streams to route to, select event stream selects to which of the output event streams to send events",
name: "route"
)