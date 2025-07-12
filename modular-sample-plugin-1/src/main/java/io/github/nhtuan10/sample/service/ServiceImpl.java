package io.github.nhtuan10.sample.service;

import io.github.nhtuan10.modular.api.Modular;
import io.github.nhtuan10.modular.api.annotation.ModularService;
import io.github.nhtuan10.modular.context.ModularContext;
import io.github.nhtuan10.sample.api.service.SampleService;
import io.github.nhtuan10.sample.api.service.SampleService2;
import io.github.nhtuan10.sample.api.service.SomeData;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

@Slf4j
@ToString
@ModularService
public class ServiceImpl extends BaseService implements SampleService {
    @Override
    public void test() {
        log.info("Service Impl: Invoke test");
        List<SampleService2> sampleService2List = Modular.getModularServices(SampleService2.class);
        for (SampleService2 sampleService2 : sampleService2List) {
            sampleService2.test();
        }
        baseMethod();
    }

    @Override
    public SomeData testReturn(SomeData in) {
        in.setName("set by sample-plugin-1 ServiceImpl#testReturn");
        return new SomeData(in.getName() + " from ServiceImpl");
    }

    @Override
    public String testObjectArray(SomeData[] in) {
        in[0].setName("set by sample-plugin-1 ServiceImpl#testReturn");
        return "ServiceImpl ->" + List.of(in);
    }

    @Override
    public List<SomeData> testObjectList(List<SomeData> in) {
//        in.forEach(d -> d.setName("set by sample-plugin-1 ServiceImpl#testObjectList"));
        for (SomeData d : in) {
            d.setName("set by sample-plugin-1 ServiceImpl#testObjectList");
        }
        var r = new ArrayList<SomeData>();
        for (SomeData d : in) {
            r.add(new SomeData(d.getName() + " result from ServiceImpl#testObjectList"));
        }
        return r;
//        return in.stream().map(d -> new SomeData( "Result from ServiceImpl#testObjectList")).toList();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public static void main(String[] args) throws InterruptedException {
        Queue<SomeData> q = Modular.getQueue("testQueue", SomeData.class);
        ModularContext.notifyModuleReady();
        while (true) {
            SomeData a = q.poll();
            log.info("Polling from testQueue: " + a);
            Thread.sleep(500);
        }
    }
}
