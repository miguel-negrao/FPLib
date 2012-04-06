(
func:{ |eventSource|
	eventSource
		.storePrevious
		.fold( Tuple2(true, true), { |previousDirs,x| Tuple2( previousDirs.at2, (x.at1-x.at2) > 0 ) })
		.select{ |x| x.at1 != x.at2 }
		.collect{ Unit }
},
desc: "Output only when going from increasing to decreasing or descreasing to increasing",
name: "changedDirection"
)
