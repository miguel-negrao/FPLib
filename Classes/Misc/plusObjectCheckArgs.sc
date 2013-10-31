+ Object {

	*checkArgs{ |class, method, args, types|
		[args, types].flopWith{ |aarg, type|
			if(aarg.isKindOf(type).not) {
				Error("%.% - Type mismatch: expected object of class % but got this: %".format(class, method, type, aarg) ).throw
			}
		}
	}

	checkArgs{ |class, method, args, types|
		Object.checkArgs(class, method, args, types)
	}

}