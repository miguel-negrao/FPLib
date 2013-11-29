+ Object {

	*checkArgs{ |class, method, args, types|
		[args, types, (0..(args.size-1))].flopWith{ |aarg, type,i|
			if(aarg.isKindOf(type).not) {
				Error("%.% - arg % Type mismatch: expected object of class % but got % of class %"
					.format(class, method, i, type, aarg, aarg.class) ).throw
			}
		}
	}

	checkArgs{ |class, method, args, types|
		Object.checkArgs(class, method, args, types)
	}

}