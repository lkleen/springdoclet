import org.junit.Test
import org.springdoclet.collectors.RequestMappingCollector

class RequestMappingCollectorTest {

  @Test
  public void makeFullPath(){
    RequestMappingCollector rmc = new RequestMappingCollector();
    def computed = rmc.makeFullPath("/heida/fsa/", "/neida/jens/")
    assert computed.equals("/heida/fsa/neida/jens")
  }

  @Test
  public void makeFullPath2(){
    RequestMappingCollector rmc = new RequestMappingCollector();

    def computed = rmc.makeFullPath("/heida/fsa", "neida/jens/")
    assert computed.equals("/heida/fsa/neida/jens")
  }
}
