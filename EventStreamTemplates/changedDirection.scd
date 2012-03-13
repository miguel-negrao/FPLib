(
func:{ |eventSource|
	eventSource
		.storePrevious
		.fold([true, true], { |previousDirs,x| [previousDirs[1], (x[0]-x[1]) > 0] })
		.select{ |array| array[0] != array[1] }
		.collect{ Unit }
},
desc: "Output only when going from increasing to decreasing or descreasing to increasing",
name: "changedDirection"
)
