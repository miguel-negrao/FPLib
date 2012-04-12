(
func:{ |eventSource, n|
	eventSource.inject(0,{ |state,x| (state+1).mod(n) });
},
desc: "iterate through n states",
name: "toggle"
)
